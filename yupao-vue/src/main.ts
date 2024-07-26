import { createApp } from 'vue'
import App from './App.vue'
import * as VueRouter from 'vue-router'
import routes from "./config/routes";
import Vant from 'vant';
import 'vant/lib/index.css';
import '../global.css'

const app=createApp(App);

app.use(Vant);


const router = VueRouter.createRouter({
    history: VueRouter.createWebHistory(),
    routes,
})


app.use(router);
app.mount('#app');





