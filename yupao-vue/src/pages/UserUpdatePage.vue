<template>
  <template v-if="user">
    <van-cell title="用户id" :value="user.userId"/>
    <van-cell title="用户昵称" is-link to="/user/edit" :value="user.userName" @click="toEdit('userName','用户昵称',user.userName)"/>
    <van-cell title="用户账户" :value="user.userAccount" />
    <van-cell title="头像" is-link to="/user/edit" >
      <img style="height: 48px;width:48px;border-radius:50%;" :src="user.avatarUrl"/>
    </van-cell>
    <van-cell title="性别" is-link to="/user/edit" :value="user.gender" @click="toEdit('gender','性别',user.gender)"/>
    <van-cell title="电话号码" is-link to="/user/edit" :value="user.phone" @click="toEdit('phone','电话号码',user.phone)"/>
    <van-cell title="电子邮箱" is-link to="/user/edit" :value="user.email" @click="toEdit('email','电子邮箱',user.email)"/>
    <van-cell title="星球编号" :value="user.planetCode" />
    <van-cell title="加入日期" :value="user.createTime" />
  </template>
</template>

<script setup lang="ts">
import {useRoute, useRouter} from "vue-router";
import {onMounted, ref} from "vue";

import {getCurrentUser} from "../services/user.ts";

const router=useRouter();

const user=ref();


onMounted(async () => {
  user.value = await getCurrentUser();
})

const toEdit=(editKey:string,editName:string,currentValue:string)=>{
  router.push({
    path:'/user/edit',
    query:{
      editKey,
      editName,
      currentValue,
    }
  })
};

// const user = {
//   userId: 1,
//   userName: 'lhy',
//   userAccount: 'lhyyy',
//   avatarUrl: 'https://cdnjson.com/images/2024/07/03/photomode_15122020_183521.png',
//   gender:'男',
//   phone: '15690332169',
//   email: 'lhyser@126.com',
//   planetCode: '1',
//   createTime: new Date()
// };

</script>

<style scoped>

</style>