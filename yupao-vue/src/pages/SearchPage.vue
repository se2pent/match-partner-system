<template>
  <form action="/">
    <van-search
        v-model="searchText"
        show-action
        placeholder="请输入要搜索的标签"
        @search="onSearch"
        @cancel="onCancel"
    />
    <van-divider v-if="activeIds.length===0">
      请选择标签
    </van-divider>
    <van-divider v-if="activeIds.length!==0">
      已选标签
    </van-divider>
    <van-space wrap>
      <van-tag :show="true" v-for="tag in activeIds"closeable size="large" type="primary" @close="doTagClose(tag)" style="margin-left: 15px">
        {{tag}}
      </van-tag>
    </van-space>


    <van-tree-select
        v-model:active-id="activeIds"
        v-model:main-active-index="activeIndex"
        :items="tagList"
    />

    <van-button type="primary" @click="doSearch" block>搜索</van-button>
  </form>
</template>

<script setup>
  import {ref} from "vue";
  import {useRouter} from "vue-router";
  const router=useRouter();

  const searchText = ref('');
  //搜索条目
  const onSearch = (val) => tagList.value=originList.map(parentTag=>{
    const tempChildren=[...parentTag.children];
    const tempParentTag={...parentTag};
    tempParentTag.children=tempChildren.filter(item=>
      item.text.includes(val)
    );

    return tempParentTag;
  });

  const onCancel = () => {
    searchText.value='';
    tagList.value=originList;
  };
  const activeIds = ref([]);
  const activeIndex = ref(0);
  const originList = [
    {
      text: '性别',
      children: [
        { text: '男生', id: '男生' },
        { text: '女生', id: '女生' },
      ],
    },
    {
      text: '年级',
      children: [
        { text: '大一', id: '大一' },
        { text: '大二', id: '大二' },
        { text: '大三', id: '大三' },
        { text: '大四', id: '大四' },
        { text: '大五', id: '大五' }
      ],
    }
  ];
  let tagList=ref(originList);

  const show = ref(true);
  const doTagClose = (tag) => {
    activeIds.value=activeIds.value.filter(item=>{
      return item!==tag;
    })
  };

  const doSearch=()=>{

    router.push({
          path:'/user/list',
          query:{
            tags:activeIds.value
          }
        })
  }
</script>

<style scoped>

</style>