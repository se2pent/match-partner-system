<template>
  <user-card-list :user-list="userList"></user-card-list>
    <van-empty image="search" v-if="!userList||userList.length<1" description="搜索结果为空" />
</template>

<script setup>
  import {useRoute} from "vue-router";
  import {onMounted, ref} from "vue";
  import myAxios from "../plugins/myAxios.js";
  import {Toast} from "vant";
  import qs from 'qs';
  import UserCardList from "../components/UserCardList.vue";



  const route = useRoute();
  const {tags} = route.query;

  const userList = ref([]);


  onMounted(async () => {
    const userListData = await myAxios.get('/user/search/tags', {
      params: {
        tagNameList: tags
      },
      paramsSerializer: params => {
        return qs.stringify(params, {indices: false})
      }
    })
        .then(function (response) {
          console.log('/user/search/tags succeed', response);
          return response?.data;
        })
        .catch(function (error) {
          console.error('/user/search/tags error', error);
          Toast.fail('请求失败');
        })
    console.log("userlistdata",userListData)
    if (userListData) {
      userListData.forEach(user => {
        if (user.tags) {
          user.tags = JSON.parse(user.tags);
        }
      })
      userList.value = userListData;

    }
  })
  // const mockUser={
  //   userId: 1,
  //   userName: 'lhy',
  //   userAccount: 'lhyyy',
  //   profile:'大家好啊我是电棍',
  //   avatarUrl:'https://cdnjson.com/images/2024/07/03/photomode_15122020_183521.png',
  //   gender:0,
  //   phone: '15690332168',
  //   email: 'lhyser@126.com',
  //   userRole: 0,
  //   tags: ['打工中','java','emo','python'],
  //   planetCode: '1234',
  //   createTime: new Date(),
  // };




</script>

<style scoped>

</style>