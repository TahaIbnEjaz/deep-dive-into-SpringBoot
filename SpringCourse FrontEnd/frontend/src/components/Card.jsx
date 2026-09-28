import axios from 'axios'
import React from 'react'
import { useNavigate } from 'react-router-dom';

const Card = ({products}) => {
  const navigate = useNavigate();

  const deleteProduct = async(id) => {
    try{
      await axios.delete(`http://localhost:8080/delete/${id}`);
      console.log("product deleted");
      navigate("/");
    }catch(e){
      console.log("error" + e)
    }
  };

  return (
    <div className="">
      
      <ul className="grid grid-cols-3 gap-4" >
        {products.map(product => (
            <div key={product.id || product.name} className='bg-cyan-900 p-4 rounded-lg shadow-lg w-[350px] ml-1.5 text-white'>
            <li>
              <img src={`http://localhost:8080/product/${product.id}/image`} 
              alt={product.name}
              className='h-48 w-full object-cover my-2 rounded-2xl' />
              <h3>{product.name}</h3>
              <h4><span className='font-bold'>Price: </span>${product.price}</h4>
              <p className='text-gray-300'><span className='font-bold text-white'>Details: </span>{product.description}</p>
              <button
                type='button'
                onClick={() => deleteProduct(product.id)}
                className='bg-red-900 p-1 px-3 rounded-[10px] cursor-pointer hover:shadow-2xs shadow-black '>Delete</button>
            </li>
            </div>
        ))}
      </ul>
    </div>
  )
}

export default Card
