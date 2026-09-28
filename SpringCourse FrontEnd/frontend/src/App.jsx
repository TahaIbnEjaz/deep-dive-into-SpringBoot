import React from 'react'
import { BrowserRouter, Routes, Route ,Link } from 'react-router-dom';
import AddProduct from './components/AddProduct';
import Home from './components/Home';

const App = () => {
  return (

      <BrowserRouter>
        <nav>
          <Link to="/add_product">
            <button className="bg-blue-500 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded mt-10">Add Product</button>
          </Link>
          <Link to="/home">
            <button className="bg-blue-500 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded mt-10 ml-3">Home</button>
          </Link>
        </nav>

        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/home" element={<Home />} />
          <Route path="/add_product" element={<AddProduct />} />  
        </Routes>  

      </BrowserRouter>

  )
}

export default App
