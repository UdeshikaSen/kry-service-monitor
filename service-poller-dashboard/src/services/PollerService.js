import axios from "axios";

const baseURL = "http://localhost:8090/services";

function getAllServices() {
  return axios.get(baseURL);
}

function createService(data) {
  return axios.post(baseURL, data);
}

function updateService(data) {
  return axios.put(`${baseURL}`, data);
}

function deleteService(id, data) {
  return axios.delete(`${baseURL}/${id}`, data);
}

export { getAllServices, createService, updateService, deleteService };
