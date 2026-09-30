# Spring Boot + Spring Security (JWT) — My Study Notes

Notes based on my `SpringCourse` project: a REST API with products (with images),
user registration/login, and JWT-protected endpoints.

**Stack:** Spring Boot · Spring Data JPA · PostgreSQL · Spring Security · jjwt 0.13

---

## Table of contents

1. Project structure
2. Configuration (`application.properties`)
3. Products and image upload/fetch
4. CORS and CSRF
5. Spring Security basics
6. Login flow with `AuthenticationManager`
7. Password encoding (BCrypt)
8. JWT: what it is and how it is built
9. The JWT filter: checking the token on every request
10. `ApplicationContext` and circular dependencies
11. Full request flow (cheat sheet)
12. Known issues in my project and how to fix them
13. Quick revision Q&A

---

## 1. Project structure

```
com.practice.SpringCourse
 ├── config        SecurityConfig, WebConfig (CORS)
 ├── controller    ProductController, UserController, StudentController
 ├── filter        JwtFilter
 ├── model         Products, Users, UserPrincipal, Student
 ├── repository    ProductRepo, UserRepo
 └── service       ProductService, UserService, JWTService, MyUserDetailsService
```

**The layers, in one line each**

| Layer | Job |
|---|---|
| Controller | Receives the HTTP request, returns the response. No business logic. |
| Service | The business logic (encode password, verify login, save image, etc.). |
| Repository | Talks to the database. Extends `JpaRepository`, so CRUD is free. |
| Model / Entity | A Java class mapped to a database table (`@Entity`). |
| Config | Beans and settings (security rules, CORS). |
| Filter | Code that runs on every request before it reaches the controller. |

Request path: `Client → Filter → Controller → Service → Repository → DB`

---

## 2. Configuration (`application.properties`)

```properties
# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/Products
spring.datasource.username=postgres
spring.datasource.password=...
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# Upload limits
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB
```

- `ddl-auto=update`: Hibernate creates or alters tables to match my entities.
  Fine for learning. In production use `validate` or a migration tool (Flyway/Liquibase).
- `show-sql` and `format_sql`: print the SQL Hibernate runs. Very useful for debugging.
- **Multipart limits:** without these, Spring rejects large uploads. `max-file-size`
  is per file, `max-request-size` is for the whole request.
- `spring.security.user.name/password` only matter for Spring's built-in default user.
  Since I wrote my own `UserDetailsService`, **these two lines do nothing** and can be deleted.
- **Never push real passwords to GitHub.** Use environment variables, for example
  `spring.datasource.password=${DB_PASSWORD}`.

---

## 3. Products and image upload/fetch

### The entity

```java
@Entity
public class Products {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String name;
    private int price;
    private String description;

    private String imageName;   // original file name
    private String imageType;   // e.g. image/png
    @Lob
    private byte[] imageData;   // the actual bytes
}
```

- `@Id` marks the primary key. `IDENTITY` lets the database auto-increment it.
- `@Lob` marks a large object column, used here to store the image bytes.
- I store three things about an image: its **name**, its **type** (so I can send the
  right `Content-Type` later), and its **bytes**.

### Uploading (`POST /product`)

```java
@PostMapping("/product")
public ResponseEntity<?> addProduct(@RequestPart Products product,
                                    @RequestPart MultipartFile image) { ... }
```

- The request must be `multipart/form-data` with two parts: `product` (JSON) and `image` (file).
- `@RequestPart` is used instead of `@RequestBody` when the request has multiple parts.
- `MultipartFile` gives access to `getOriginalFilename()`, `getContentType()` and `getBytes()`.

Service:

```java
product.setImageName(image.getOriginalFilename());
product.setImageType(image.getContentType());
product.setImageData(image.getBytes());
return productRepo.save(product);
```

In Postman: Body → form-data → key `product` (type **Text**, and set its content type
to `application/json`) and key `image` (type **File**).

### Fetching (`GET /product/{id}/image`)

```java
return ResponseEntity.ok()
        .contentType(MediaType.valueOf(product.getImageType()))
        .body(product.getImageData());
```

- Returns raw bytes with the right content type, so a browser or an `<img src="...">`
  can display it directly.
- `@PathVariable` pulls `{id}` out of the URL.

### Other things used

- `ResponseEntity`: lets me control the HTTP status (`201 CREATED`, `500`, etc.) along with the body.
- `findById(id).orElseThrow(...)`: `findById` returns an `Optional`, so I decide what
  happens if nothing is found.

---

## 4. CORS and CSRF

### CORS (Cross-Origin Resource Sharing)

Browsers block a frontend on one origin (`localhost:5173`) from calling an API on another
(`localhost:8080`) unless the server allows it.

```java
registry.addMapping("/**")
        .allowedOrigins("http://localhost:5173")
        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
        .allowedHeaders("*")
        .allowCredentials(true);
```

- Set globally in `WebConfig` (implements `WebMvcConfigurer`), or per controller with `@CrossOrigin`.
- Doing both is redundant. One place is enough.
- CORS is enforced by the **browser**. Postman ignores it.

### CSRF (Cross-Site Request Forgery)

An attack where another site tricks a logged-in browser into sending a request to my site
(the browser attaches the session cookie automatically).

- Spring Security turns CSRF protection on by default.
- I disabled it with `.csrf(AbstractHttpConfigurer::disable)`.
- That is **fine for a stateless JWT API**, because the token is sent manually in a header
  (not automatically like a cookie), so the attack does not work the same way.
- If sessions and cookies are used, keep CSRF enabled.
- Side effect: `/csrf-token` in `StudentController` returns `null` now, because CSRF is disabled.

---

## 5. Spring Security basics

### What happens the moment I add the dependency

`spring-boot-starter-security` locks **every** endpoint behind a login and generates a random
password in the console. Everything after that is me customizing it.

### The Security Filter Chain

Spring Security is a chain of servlet filters that every request passes through before
reaching a controller.

```
Request → [Filter 1] → [Filter 2] → ... → [JwtFilter] → ... → Controller
```

I configure the chain with a `SecurityFilterChain` bean:

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) {
    return http
        .csrf(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(request -> request
            .requestMatchers("/register", "/login").permitAll()
            .anyRequest().authenticated())
        .httpBasic(Customizer.withDefaults())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
        .build();
}
```

| Line | Meaning |
|---|---|
| `csrf(disable)` | Turn off CSRF protection (OK for stateless APIs). |
| `requestMatchers(...).permitAll()` | Anyone can call `/register` and `/login` without logging in. |
| `anyRequest().authenticated()` | Every other URL needs a logged-in user. |
| `httpBasic(...)` | Also allow HTTP Basic auth (username and password in a header). |
| `STATELESS` | Server keeps **no session**. Every request must prove who it is. |
| `addFilterBefore(jwtFilter, ...)` | Run my JWT filter before Spring's username/password filter. |

Rules are checked **top to bottom**, so specific rules go first and `anyRequest()` goes last.

### Stateless vs stateful

| | Session (stateful) | JWT (stateless) |
|---|---|---|
| Server remembers user? | Yes (session id in a cookie) | No |
| What the client sends | Session cookie | `Authorization: Bearer <token>` |
| Scales across servers | Harder | Easier |

### The key Spring Security objects

| Object | Role |
|---|---|
| `UserDetails` | Spring's view of a user: username, password, authorities, account flags. |
| `UserDetailsService` | Loads a `UserDetails` from my database by username. |
| `AuthenticationProvider` | Does the actual checking (is the password right?). |
| `AuthenticationManager` | The front door. Delegates to the providers. |
| `Authentication` | An object representing "who is this user and are they authenticated?" |
| `SecurityContextHolder` | Where the current request's `Authentication` is stored. |
| `PasswordEncoder` | Hashes passwords and checks a raw password against a hash. |

### `UserPrincipal` (my `UserDetails`)

`Users` is my database entity. `UserPrincipal` **wraps** it so Spring can understand it:

```java
public class UserPrincipal implements UserDetails {
    private Users user;
    public String getUsername() { return user.getUserName(); }
    public String getPassword() { return user.getPassword(); }
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singleton(new SimpleGrantedAuthority("USER"));
    }
    // isAccountNonExpired / NonLocked / CredentialsNonExpired / isEnabled -> true
}
```

- Keeping the entity and the security object separate is good design.
- The four `is...` methods let me lock or expire accounts later. Returning `true` means "no restriction."
- Roles convention: authorities used with `hasRole("USER")` should be named `ROLE_USER`.

### `MyUserDetailsService`

```java
public UserDetails loadUserByUsername(String username) {
    Users user = userRepo.findByUserName(username);
    if (user == null) throw new UsernameNotFoundException("User Not Found");
    return new UserPrincipal(user);
}
```

- Spring calls this whenever it needs to look up a user.
- `findByUserName` is a **derived query**: Spring Data writes the SQL from the method name
  (`findBy` + field name `userName`).

---

## 6. Login flow with `AuthenticationManager`

### Register (`POST /register`)

```java
user.setPassword(encoder.encode(user.getPassword()));
return userRepo.save(user);
```

Passwords are **hashed before saving**, never stored as plain text.

### Login (`POST /login`)

```java
Authentication authentication = authManager.authenticate(
    new UsernamePasswordAuthenticationToken(user.getUserName(), user.getPassword()));

if (authentication.isAuthenticated()) {
    return jwtService.generateToken(user.getUserName());
}
```

What happens inside `authenticate(...)`:

```
AuthenticationManager (ProviderManager)
   └─► DaoAuthenticationProvider
         1. MyUserDetailsService.loadUserByUsername()   → user + stored hash from DB
         2. passwordEncoder.matches(rawPassword, storedHash)
         3. Match  → returns an authenticated Authentication
            No     → throws BadCredentialsException
```

Points to remember:

- `UsernamePasswordAuthenticationToken(username, password)` with two arguments is an
  **unauthenticated** token (a "please check this" request).
- `authenticate()` **throws** on failure. That is why the code uses `try/catch (AuthenticationException)`.
- The JWT is created **only after** authentication succeeds.

### Wiring the provider

```java
@Bean
public AuthenticationProvider authenticationProvider(UserDetailsService uds) {
    DaoAuthenticationProvider provider = new DaoAuthenticationProvider(uds);
    provider.setPasswordEncoder(new BCryptPasswordEncoder(12));
    return provider;
}

@Bean
public AuthenticationManager authenticationManager(AuthenticationConfiguration config) {
    return config.getAuthenticationManager();
}
```

I expose the `AuthenticationManager` as a bean so I can `@Autowired` it into `UserService`.

### The bug I hit earlier (worth remembering)

```java
// WRONG: JSON body is not read, so userName and password are null
public String login(Users user)

// RIGHT
public String login(@RequestBody Users user)
```

Without `@RequestBody`, Spring does not read the JSON. Everything arrives `null` and login
always fails. If login fails for a correct password, first check that the request body
actually reached the object.

---

## 7. Password encoding (BCrypt)

```java
new BCryptPasswordEncoder(12)
```

- BCrypt is a **one-way** hash. You cannot get the password back from the hash.
- It adds a random **salt** automatically, so the same password produces a different hash each time.
- The `12` is the **strength** (cost factor). Each +1 doubles the work. Higher means
  slower for attackers, but also slower for me.
- Checking a login is `encoder.matches(rawPassword, hash)`. You never hash the input and compare strings yourself.
- Users registered *before* encoding was added have plain-text passwords in the DB and will
  never match. Delete and re-register them.
- Cleaner design: one `@Bean PasswordEncoder` injected everywhere, instead of creating
  `new BCryptPasswordEncoder(12)` in two places.

---

## 8. JWT: what it is and how it is built

### The idea

1. Log in once with username and password.
2. Server returns a **signed token**.
3. Client sends the token with every later request.
4. Server checks the signature. If valid, it trusts the token without asking for the password again.

### Structure: three Base64URL parts separated by dots

```
header.payload.signature
```

| Part | Contains | Example |
|---|---|---|
| Header | Signing algorithm | `{"alg":"HS256"}` |
| Payload | Claims (data) | `{"sub":"taha","iat":...,"exp":...}` |
| Signature | `HMAC-SHA256(header.payload, secretKey)` | binary, Base64-encoded |

- `sub` = subject (who the token is about), `iat` = issued at, `exp` = expiry.
- **The payload is NOT encrypted.** Anyone can decode it (try jwt.io). Never put
  passwords or secrets in it.
- The signature does not hide data. It **prevents tampering**. If someone edits the payload, the
  signature no longer matches and the server rejects the token. Only the server knows the secret key.

### `JWTService` explained

**Creating the key (constructor, runs once at startup)**

```java
KeyGenerator keyGen = KeyGenerator.getInstance("HmacSHA256");
SecretKey sk = keyGen.generateKey();
secretKey = Base64.getEncoder().encodeToString(sk.getEncoded());
```

Generates a random 256-bit key and stores it as a Base64 string.

**Building the token**

```java
Jwts.builder()
    .claims().add(claims).subject(userName)
             .issuedAt(new Date(System.currentTimeMillis()))
             .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 30))
    .and()                 // close the claims builder
    .signWith(getKey())    // compute the signature
    .compact();            // join into "header.payload.signature"
```

- Time in Java `Date` is in **milliseconds**: `1000 * 60 * 30` = 30 minutes.
  (I once forgot the `1000` and my token lived 108 seconds.)

**Getting the key back**

```java
byte[] keyBytes = Decoders.BASE64.decode(secretKey);
return Keys.hmacShaKeyFor(keyBytes);
```

`hmacShaKeyFor` picks the algorithm from the key length: 32 bytes gives HS256, 48 gives HS384,
64 gives HS512. That is why `signWith(getKey())` needs no algorithm argument.

**Reading and verifying a token**

```java
Jwts.parser()
    .verifyWith(getKey())       // recompute signature; reject if it doesn't match
    .build()
    .parseSignedClaims(token)   // also throws ExpiredJwtException if past exp
    .getPayload();              // the Claims
```

**The generic claim helper**

```java
private <T> T extractClaim(String token, Function<Claims, T> claimResolver) {
    Claims claims = extractAllClaims(token);
    return claimResolver.apply(claims);
}

extractClaim(token, Claims::getSubject);      // username
extractClaim(token, Claims::getExpiration);   // expiry date
```

Parse once, then pull out whichever claim is needed by passing a method reference.
This is a Java `Function` (functional interface) in action.

**Validating**

```java
return userName.equals(userDetails.getUsername()) && !isTokenExpired(token);
```

Two checks: the token belongs to this user, and it has not expired.
(The signature is already checked during parsing.)

---

## 9. The JWT filter: checking the token on every request

```java
@Component
public class JwtFilter extends OncePerRequestFilter {

    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        String token = null, userName = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);              // strip "Bearer "
            userName = jwtService.extractUserName(token);
        }

        if (userName != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = ...loadUserByUsername(userName);

            if (jwtService.validateToken(token, userDetails)) {
                UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        filterChain.doFilter(request, response);   // ALWAYS continue the chain
    }
}
```

### Step by step

1. Read the `Authorization` header. Expected format: `Bearer <token>`.
2. `substring(7)` removes the 7 characters of `"Bearer "`.
3. Extract the username from the token (this also verifies signature and expiry).
4. If there is a username and nobody is authenticated yet on this request, load the user from the DB.
5. Validate the token against that user.
6. Build a **three-argument** `UsernamePasswordAuthenticationToken(principal, credentials, authorities)`.
   The three-argument version is marked **authenticated**.
7. Store it in `SecurityContextHolder`. Now Spring's `.authenticated()` rule sees "logged in."
8. `filterChain.doFilter(...)` passes the request to the next filter and eventually the controller.

### Rules that matter

- **Always call `filterChain.doFilter`**, outside every `if`. Skip it and the request dies here.
- `OncePerRequestFilter` guarantees the filter runs once per request.
- `@Component` is required, or Spring won't create the filter as a bean.
- `addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)` places it in the chain.
- If the token is missing or invalid, the filter just doesn't authenticate. The later
  `.authenticated()` rule then rejects the request (401/403).
- `SecurityContextHolder` is per request (thread-local). With stateless sessions it is empty
  again on the next request, which is why the filter must rebuild it every time.

---

## 10. `ApplicationContext` and circular dependencies

`ApplicationContext` **is** the Spring container: it holds all beans.
`@Autowired` asks it for beans automatically. `context.getBean(X.class)` asks manually.

### Why courses use it in `JwtFilter`

A possible **circular dependency** at startup:

```
SecurityConfig ──needs──► JwtFilter ──needs──► MyUserDetailsService
       ▲                                              │
       └────────── needs UserDetailsService ◄─────────┘
```

`getBean()` runs at request time, when all beans already exist, so it dodges the startup cycle.

### Better options

1. **Constructor injection** (cleanest and easiest to test):
   ```java
   private final JWTService jwtService;
   private final MyUserDetailsService uds;
   public JwtFilter(JWTService jwtService, MyUserDetailsService uds) { ... }
   ```
2. **Remove unused injections.** My `SecurityConfig` has an `@Autowired UserDetailsService`
   field that is never used (the provider bean takes it as a method parameter instead).
3. **`@Lazy`** on one dependency: injects a proxy and creates the real bean on first use.

`ApplicationContext` works, but it hides dependencies and is harder to test.

---

## 11. Full request flow (cheat sheet)

### A) Registering

```
POST /register {userName, password}
  → permitAll (no token needed)
  → UserService.addUser: hash password with BCrypt → save to DB
```

### B) Logging in

```
POST /login {userName, password}
  → permitAll
  → UserService.verify
      → AuthenticationManager.authenticate
          → MyUserDetailsService loads user from DB
          → BCrypt matches password with stored hash
      → JWTService.generateToken(userName)
  ← returns the JWT string
```

### C) Calling a protected endpoint

```
GET /ShowProducts
Header: Authorization: Bearer <token>
  → JwtFilter
      → read the header, strip "Bearer "
      → verify signature and expiry, read username
      → load user, validate token
      → put Authentication in SecurityContextHolder
      → filterChain.doFilter(...)
  → authorizeHttpRequests: authenticated? yes → allowed
  → ProductController → ProductService → DB
  ← response
```

### D) Token missing, invalid or expired

```
Filter does not authenticate → .authenticated() rule fails → 401/403
```

### Testing in Postman

1. `POST /register` with JSON body.
2. `POST /login` with the same JSON. Copy the token from the response.
3. On any other request: Authorization tab → Bearer Token → paste, or add the header manually.

---

## 12. Known issues in my project and how to fix them

Things I noticed reading my final code. None stop it from working in the happy path,
but they matter for real apps.

| # | Issue | Why it matters | Fix |
|---|---|---|---|
| 1 | Invalid, expired or tampered token → `extractUserName` throws `JwtException` inside the filter | The request fails with a 500 error instead of a clean 401 | Wrap the token parsing in `try/catch (JwtException \| IllegalArgumentException)` and leave the request unauthenticated |
| 2 | Secret key is generated randomly on every startup | Restart the app and every issued token becomes invalid | Store a fixed secret in config or an environment variable and read it with `@Value` |
| 3 | `/register` returns the whole `Users` entity | Response contains the password hash | Return a DTO, or clear the password before returning |
| 4 | Authority named `"USER"` | `hasRole("USER")` expects `ROLE_USER` | Use `new SimpleGrantedAuthority("ROLE_USER")` |
| 5 | Two separate `BCryptPasswordEncoder` instances | Works only because both use strength 12 | One `@Bean PasswordEncoder`, injected where needed |
| 6 | Unused `@Autowired UserDetailsService` in `SecurityConfig` | Can cause circular-dependency headaches | Delete it |
| 7 | `spring.security.user.*` in properties | Ignored because I have a custom `UserDetailsService` | Delete those lines |
| 8 | DB password committed in properties | Security risk if the repo is public | Use environment variables |
| 9 | `ProductService.deleteItem` wraps `deleteById` in `try/catch` | In recent Spring Data versions, `deleteById` on a missing id does nothing (no exception), so the "not found" branch never runs | Check with `existsById(id)` first and throw if missing |
| 10 | `/csrf-token` returns `null` | CSRF is disabled, so there is no `_csrf` attribute | Remove the endpoint or enable CSRF |
| 11 | `home()` calls `request.getSession()` | Creates a session, which goes against the stateless design | Remove it from a stateless API |
| 12 | `@Lob byte[]` on PostgreSQL | Hibernate can map this to Postgres large objects (`oid`), which behave differently from plain `bytea` and can cause "large objects may not be used in auto-commit mode" errors in some setups | If I hit that error, look at mapping the field as `bytea` instead |
| 13 | `System.out.println` in `verify` | Debug leftovers; `getCause()` is often `null` | Remove, or use a logger |
| 14 | `login` returns the string `"failed"` with HTTP 200 | Clients can't tell success from failure by status code | Return `401 Unauthorized` via `ResponseEntity` |
| 15 | Unused imports (`NoOpPasswordEncoder`, `ConfigurationPropertyCaching`, etc.) | Clutter | Optimize imports in the IDE |

**What to learn next:** roles and method security (`@PreAuthorize`, `hasRole`), refresh tokens,
a global exception handler (`@ControllerAdvice`), DTOs and validation (`@Valid`), and a custom
`AuthenticationEntryPoint` for clean 401 responses.

---

## 13. Quick revision Q&A

**Why is the JWT payload not secret?**
It is only Base64-encoded, not encrypted. The signature protects integrity, not privacy.

**What does the signature protect against?**
Tampering. Changing any character of the header or payload breaks the signature.

**Why must the filter call `filterChain.doFilter`?**
Otherwise the request never reaches the next filter or the controller.

**What is `SecurityContextHolder`?**
Per-request storage for the current user's `Authentication`. It is how the rest of Spring
Security knows the request is authenticated.

**Difference between `UsernamePasswordAuthenticationToken` with 2 vs 3 arguments?**
Two arguments creates an *unauthenticated* token (used for login). Three arguments (with
authorities) creates an *authenticated* one (used in the filter).

**Why STATELESS?**
No server-side sessions. Each request carries its own proof (the token).

**Why is CSRF disabled for JWT APIs?**
The token is sent manually in a header rather than automatically like a cookie, so the classic
CSRF attack doesn't apply.

**What throws when the password is wrong?**
`authenticate()` throws `BadCredentialsException` (a subclass of `AuthenticationException`).

**Why hash passwords instead of encrypting them?**
Hashing is one-way. Even if the database leaks, the original passwords can't be recovered
directly.

**`@RequestBody` vs `@RequestPart`?**
`@RequestBody` reads the whole body as JSON. `@RequestPart` reads one part of a
multipart request (used for file upload plus JSON).

**What is a derived query?**
A repository method whose name Spring Data turns into SQL, for example `findByUserName`.

**Why did my login always fail earlier?**
`@RequestBody` was missing on the `/login` parameter, so username and password were `null`.

---
*End of notes.*
