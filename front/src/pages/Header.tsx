import React from "react";
import { useLocation } from "react-router-dom";
import AlbumHeader from "../components/Header/AlbumHeader";
import BackHeader from "../components/Header/BackHeader";
import LogoHeader from "../components/Header/LogoHeader";
import WriteHeader from "../components/Header/WriteHeader";
import SaveHeader from "../components/Header/SaveHeader";

const Header: React.FC = () => {
  const location = useLocation()

  const basicPath = ['/', '/group', '/mypage',]
  const backPath = ['/grouplist', '/setting', '/feed','/groupcreate', '/chat']
  const albumPath = ['/album']

  if (basicPath.includes(location.pathname)) return <LogoHeader />
  else if (location.pathname.startsWith('/feed/img/')) return <SaveHeader />
  else if (backPath.includes(location.pathname) || location.pathname.startsWith('/album/') || location.pathname.startsWith('/feed/')) return <BackHeader />
  else if (albumPath.includes(location.pathname)) return <AlbumHeader />
  else if (location.pathname === '/write') return <WriteHeader />
};

export default Header;