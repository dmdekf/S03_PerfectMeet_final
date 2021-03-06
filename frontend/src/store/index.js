import Vue from "vue";
import Vuex from "vuex";

import axios from "axios";

import router from "@/router";
import SERVER from "@/api/api";
import createPersistedState from "vuex-persistedstate";

// const storagesession = window.sessionStorage;
// const storagelocal = window.localStorage;
Vue.use(Vuex);
export default new Vuex.Store({
  state: {
    token: "",
    email: "",
    status: "",
    nickname: "",
    login_user: "",
    auth_token: "",
    select_map: "",
    store_id:"",
    select_userpurpose: "",
    data_list: "",
    board_lists: []
  },
  plugins: [createPersistedState()],
  getters: {
    info: (state) => ({
      status: state.status,
      token: state.token,
      auth_token: state.auth_token,
      store_id: state.store_id
    }),
    isLoggedIn: (state) => !!state.token,
    nickname: (state) => {
      return state.nickname;
    },
    store_id: (state) => {
      return state.store_id;
    }
  },

  mutations: {
    SET_AUTHTOKEN(state, { auth_token }) {
      state.auth_token = auth_token;
    },
    SET_TOKEN(state, { token }) {
      state.token = token;
    },
    SET_STOREID(state, { store_id }) {
      state.store_id = store_id;
    },
    SET_EMAIL(state, { email }) {
      state.email = email;
    },
    SET_STATUS(state, { status }) {
      state.status = status;
    },
    SET_NICKNAME(state, { nickname }) {
      state.nickname = nickname;
    },
    SET_BOARDLISTS(state, { board_lists }) {
      state.board_lists = board_lists;
    },
  },
  actions: {
    postAuthData({ commit }, info) {
      axios
        .post(SERVER.URL + info.location, info.data)
        .then((res) => {
          commit("SET_TOKEN", res.headers["jwt-auth-token"]);
          router.push({ name: "Home" });
        })
        .catch((err) => console.log(err.response.data));
    },
    signup({ getters },signupData) {
      console.log(getters);
      console.log(signupData);
      axios({
        method: "post",
        url: SERVER.URL + "/user",
        data: {
          email: signupData.email,
          nickname: signupData.nickname,
          password: signupData.password,
        },
      })
        .then((res) => {
          if (res.data.status) {
            alert("?????? ???????????? ?????????????????????. ????????? ??????????????????!")
            router.push({ name: "MAIN" });
          }
        })
        .catch((err) => console.log(err.response.data));
    },

    login({ commit, getters }, loginData) {
      console.log(loginData);
      axios({
        method: "post",
        url: SERVER.URL + "/user/login",
        data: {
          email: loginData.loginEmail,
          password: loginData.loginPassword,
        },
      })
        .then((res) => {
          console.log(res);
          if (res.data.status) {
            commit("SET_TOKEN", { token: res.headers["jwt-auth-token"] });
            commit("SET_NICKNAME", {
              nickname: res.headers["nickname"]
            });
            // commit("SET_STOREID", {
            //   store_id: parseInt(res.headers["store_id"])
            // });
            axios({
              method: "get",
              url: SERVER.URL + "/feature/storeres/findStoreName",
              params: {
                nickname: res.headers["nickname"]
              },
            })
              .then((rres) => {
                console.log("Store info")
                console.log(rres);
                if (rres.data.status) {
                  
                  commit("SET_STOREID", {
                    store_id: rres.data.data.id
                  });
                  
                }
              })
              .catch((e) => {
                console.log(e);
              });


            getters.config;
            router.push({ name: "MAIN" });
          }
        })
        .catch((e) => {
          console.log(e.response.data);
        });

        
        
    },
    logout({ commit }) {
      
      commit("SET_TOKEN", { token: "" });
      commit("SET_NICKNAME", { nickname: "" });
      commit("SET_STATUS", { status: "" });
      commit("SET_STOREID", {store_id : null})
      router.push({
          name: "MAIN"
        });
      alert('???????????? ???????????????.')
    },
    getBoardLists({
      state
    }) {
      axios({
        method: "get",
        url: SERVER.URL + "/feature/board/list"
      })
        .then((res) => {
          if (res.data) {
            console.log(res.data)
            state.board_lists = res.data
          }
        })
        .catch((error)=> {
          if (error.response) {
            console.log(error.response.data);
          } else {
            console.log(error.request);
          }
        })
    },
  },
});