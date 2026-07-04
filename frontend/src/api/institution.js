import axios from 'axios'

export function getInstitutions(params = {}) {
  return axios.get('/api/institutions', { params }).then(res => res.data)
}