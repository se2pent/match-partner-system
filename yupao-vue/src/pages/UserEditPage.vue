<template>
  <van-form @submit="onSubmit">
    <van-cell-group inset>
      <van-field
          v-model="editUser.currentValue"
          :name="editUser.editKey"
          :label="editUser.editName"
          :placeholder="`请输入${editUser.editName}`"
      />
    </van-cell-group>
    <div style="margin: 16px;">
      <van-button round block type="primary" native-type="submit">
        提交
      </van-button>
    </div>
  </van-form>
</template>

<script setup >
import {useRoute, useRouter} from "vue-router";
  import {ref} from "vue";
  import myAxios from "../plugins/myAxios.js";
  import axios from "axios";
  import {Toast} from "vant";
import {getCurrentUserState} from "../state/user.ts";
import {getCurrentUser} from "../services/user.ts";

  const route=useRoute();
  const router=useRouter();
  console.log("query",route.query);

  const editUser=ref({
    editKey:route.query.editKey,
    editName:route.query.editName,
    currentValue:route.query.currentValue
  });


  const onSubmit = async (values) => {
  const currentUser=await getCurrentUser();
    if (!currentUser){
      Toast.fail("用户未登录")
      return;
    }
    const res=await myAxios.post('/user/update', {
          userId:currentUser.userId,
          [editUser.value.editKey]:editUser.value.currentValue
        }
    )
    if (res.code===0&&res.data>0){
      Toast.fail("修改成功");
      router.back();
    }else {
      Toast.success("修改失败")
    }
    console.log('submit', values);
  };
</script>

<style scoped>

</style>