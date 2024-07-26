import axios from "axios";


const isDev = process.env.NODE_ENV === 'development';

const myAxios = axios.create({
    baseURL: isDev ? 'http://localhost:8080/api' : 'http://42.193.112.225:8080/api',
});

myAxios.defaults.withCredentials=true;

myAxios.interceptors.request.use(function (config){
    console.log('我要发请求啦',config)


    return config;
},function (error){
    return Promise.reject(error)
});

myAxios.interceptors.response.use(function (response){
    console.log('我收到你的响应啦',response);
    // response.data.data.expireTime=new Date(response.data.data.expireTime)
    // console.log("过滤器",response.data.data.expireTime)
    if (response?.data?.code === 40100) {
        const redirectUrl = window.location.href;
        window.location.href = `/user/login?redirect=${redirectUrl}`;
    }
    return response.data;
},function (error){
    return Promise.reject(error);
});


export default myAxios;