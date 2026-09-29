import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class JDBC {
    public static void main(String[] args) throws Exception {

        String sql = "Select * from users";
        String url = "jdbc:postgresql://localhost:5432/JDBC";
        String username = "postgres";
        String password = "tumbatao";

        Connection con = DriverManager.getConnection(url , username , password);

        Statement st = con.createStatement();
        ResultSet resultSet =  st.executeQuery(sql);

        resultSet.next();

        String data = resultSet.getString(3);

        System.out.println(data);

        con.close();
    }
}