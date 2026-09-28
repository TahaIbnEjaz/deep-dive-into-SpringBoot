import { useState, useEffect } from 'react';
import axiosInstance from '../api/axiosInstance';
import Card from './Card';

const API_BASE_URL = "http://localhost:8080"

const Home = () => {

  const[products,setProducts] = useState([])

  useEffect(() => {
    axiosInstance.get('/ShowProducts')
      .then(response => {
        setProducts(response.data);
        console.log(response.data)
      })
      .catch(error => {
        console.log("error fetching data" , error)
      })
  }, []);

  return (
    <div>
      <h1>SpringCourse FrontEnd</h1>
      <p>Learning to add image from Spring boot</p>

      <br />
      
      <div >
        <Card products={products} />
      </div>
      
          
    </div>
  )
}

export default Home
