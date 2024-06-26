import { Routes, Route, useNavigate } from 'react-router-dom';
import { useStore } from 'zustand';
import { DetailImgStore } from './stores/DetailImgStore';
import MainLayout from './MainLayout'
import Main from './pages/Main';
import Login from './pages/Login';
import Camera from './pages/Camera';

import GroupMain from './pages/GroupMain';
import GroupList from './pages/GroupList';
import GroupCreate from './pages/GroupCreate';
import GroupAttendLogin from './pages/GroupAttendLogin';
import GroupAttend from './pages/GroupAttend';
import GroupConfig from './pages/GroupConfig/GroupConfig';
import GroupEnvConfig from './pages/GroupConfig/GroupEnvConfig';
import GroupProfile from './pages/GroupConfig/GroupProfileChange';
import GroupBan from './pages/GroupConfig/GroupBan';
import GroupInfo from './pages/GroupConfig/GroupInfo';
import GroupUser from './pages/GroupConfig/GroupUser';
import GroupUserDetail from './pages/GroupConfig/GroupUserDetail';
import GroupPass from './pages/GroupConfig/GroupPass';

import Setting from './pages/Setting';
import Album from './pages/Album';
import ImgGroup from './pages/ImgGroup';
import Feed from './pages/Feed';
import DetailImg from './pages/DetailImg';
import ArticleCreate from './pages/ArticleCreate';
import ArticleModify from './pages/ArticleModify';
import Chatting from './pages/Chatting';
import ArticleDetail from './pages/ArticleDetail';
import ArticleImg from './pages/ArticleImg';
import Trash from './pages/Trash';

import LoginRedirect from './components/User/LoginRedirect';
import SimplePass from './pages/SimplePass';
import ErrorRedirect from './pages/ErrorRedirect';
import { userStore } from './stores/UserStore';
import { guestStore } from './stores/GuestStore';
import { useEffect } from 'react';
import { optionStore } from './stores/OptionStore';

export default function Router() {
  const { currentImg } = useStore(DetailImgStore)
  const user = useStore(userStore);
  const guest = useStore(guestStore);
  const opt = useStore(optionStore);
  const navi = useNavigate();

  useEffect(() => {
    if(!user.isLogin && !guest.isGuest && !guest.isInvite && !location.pathname.includes('/attend')) {
      navi('/login');
    }
  }, [location.pathname, navi])

  useEffect(() => {
    if(user.uploadGroupId && opt.startPage === 'camera') {
      navi('/camera')
    }
  },[])

  return (
    <Routes location={location} key={location.pathname}>
      {/* 헤더 & 네브를 넣을 페이지 */}
      <Route element={<MainLayout />}> 
        <Route path="/" element={<Main />} />

        {/*그룹 생성 및 목록*/}
        <Route path="/group/create" element={<GroupCreate />} />
        <Route path="/group/list" element={<GroupList />} />

        {/*그룹 기본*/}
        <Route path="/group/:groupPk/main" element={<GroupMain />} />

        {/*그룹 참여*/}
        <Route path="/group/:groupPk/attend" element={<GroupAttend />} />
        <Route path="/group/:groupPk/attend/login" element={<GroupAttendLogin />} />

        {/*그룹 설정*/}
        <Route path="/group/:groupPk/config" element={<GroupConfig />} />
        <Route path="/group/:groupPk/env" element={<GroupEnvConfig />} />
        <Route path="/group/:groupPk/profile" element={<GroupProfile />} />
        <Route path="/group/:groupPk/ban" element={<GroupBan />} />
        <Route path="/group/:groupPk/info" element={<GroupInfo />} />
        <Route path="/group/:groupPk/user" element={<GroupUser />} />
        <Route path="/group/:groupPk/userdetail" element={<GroupUserDetail />} />
        <Route path="/group/:groupPk/pass" element={<GroupPass />} />

        <Route path="/setting" element={<Setting />} />
        <Route path="/album/:groupPk" element={<Album />} />
        <Route path="/imagegroup/:groupPk" element={<ImgGroup />} />
        <Route path={`album/detail/${currentImg}/:groupPk`} element={<DetailImg />} />
        <Route path="/feed/:groupPk" element={<Feed />} />
        <Route path="/feed/:groupPk/detail/:feedPk" element={<ArticleDetail />} />
        <Route path="/feed/:groupPk/detail/:feedPk/:imgPk" element={<ArticleImg />} />
        <Route path="/chat/:groupPk" element={<Chatting />} />
        <Route path="/trash/:groupPk" element={<Trash />} />
      </Route>

      {/* 헤더 & 네브가 필요 없는 페이지 */}
      <Route path="/login" element={<Login />} />
      <Route path="/loginredirect" element={<LoginRedirect />} />
      <Route path="/camera" element={<Camera />} />
      <Route path='/simpleinsert' element={<SimplePass type='insert'/>}/>
      <Route path='/simplecheck' element={<SimplePass type='check'/>}/>
      <Route path="/write/:groupPk" element={<ArticleCreate />} />
      <Route path="/modify/:groupPk/:feedPk" element={<ArticleModify />} />
      <Route path='/error' element={<ErrorRedirect />}/>
    </Routes>
  );
}
