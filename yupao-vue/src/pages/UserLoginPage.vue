<template>
  <van-form @submit="onSubmit">
    <van-cell-group inset>
      <van-field
          v-model="userAccount"
          name="用户名"
          label="用户名"
          placeholder="用户名"
          :rules="[{ required: true, message: '请填写用户名' }]"
      />
      <van-field
          v-model="userPassword"
          type="password"
          name="密码"
          label="密码"
          placeholder="密码"
          :rules="[{ required: true, message: '请填写密码' }]"
      />
    </van-cell-group>
    <div style="margin: 16px;">
      <van-button round block type="primary" native-type="submit">
        提交
      </van-button>
    </div>
  </van-form>
</template>

<script setup lang="ts">
  import {useRoute, useRouter} from "vue-router";
  import {ref} from "vue";
  import myAxios from "../plugins/myAxios.js";
  import axios from "axios";
  import {Toast} from "vant";

  const route=useRoute();
  const router=useRouter();

  const userAccount = ref('');
  const userPassword = ref('');
  const onSubmit = async () => {
    const res=await myAxios.post('/user/login',{
      userAccount:userAccount.value,
      userPassword:userPassword.value
    });
    console.log("res",res)
    if (res.code===0&&res.data){
      Toast.success("登录成功")
      const redirectUrl = route.query?.redirect as string ?? '/';
      window.location.href = redirectUrl;
    } else {
      Toast.fail("登录失败")
    }
  };

</script>

<style scoped>

</style>