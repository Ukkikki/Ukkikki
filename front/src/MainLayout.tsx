import { Outlet } from "react-router-dom";
import Header from "./components/commons/Header";
import Hamburger from "./components/commons/Hamburger";

// 헤더가 필요한 곳에 대한 설정
function App() {
	return (
		<div>
      <div className="w-screen h-screen">
        <Header />

        <div className="fixed top-14 w-full h-full overflow-scroll scrollbar-hide">
          <Outlet />
          <Hamburger />
        </div>
			</div>
		</div>
	);
}

export default App;
