import axios from 'axios';
import React from 'react'
import { useState } from 'react'

const AddProduct = () => {

    const[product,setProduct] = useState({
        name:"",
        price:"",
        description:"",
    })

    const[image,setImage] = useState(null);

    const handleInputChange = (e) => {
        const{name ,value} = e.target;
        setProduct({...product, [name]:value});
    }

    const handleImageChange = (e) => {
      setImage(e.target.files[0]);
    }

    const submitHandler = (event) => {
      event.preventDefault();
      const formData = new FormData();
      formData.append(
      "product",
      new Blob([JSON.stringify(product)], { type: "application/json" })
    );
      formData.append("image", image);

      axios
      .post("http://localhost:8080/product", formData, {
        headers: {
          "Content-Type": "multipart/form-data",
        },
      })
      .then((response) => {
        console.log("Product added successfully:", response.data);
        alert("Product added successfully");
      })
      .catch((error) => {
        console.error("Error adding product:", error);
        alert("Error adding product");
      });
    }



  return (
    <div className='flex flex-col items-center justify-center gap-5 bg-cyan-950 w-[50%] h-[60vh] rounded-2xl ml-[25%] border-solid border-2 border-cyan-500 mt-10'>
      form to add product
      <form onSubmit={submitHandler}>
        <input name='name' value={product.name} onChange={handleInputChange} type="text" placeholder='Enter Product Name' />
        <input name='price' value={product.price} onChange={handleInputChange} type="number" placeholder='Enter Product Price' />
        <input name='description' value={product.description} onChange={handleInputChange} type="text" placeholder='Enter Product Description' />
        <input onChange={handleImageChange} type="file" accept='.jpg,.jpeg,.png' placeholder='Enter Product Image URL' className='bg-gray-900' />
        <button type="submit" className='bg-gray-500 hover:bg-gray-700 text-white font-bold py-2 px-4 rounded w-[60%] ml-15'>Add</button>
      </form>
    </div>
  )
}

export default AddProduct
