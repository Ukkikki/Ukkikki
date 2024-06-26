import React, { useEffect, useState } from "react";
import settings from "@/assets/GroupMain/settings.png"
import etc from "@/assets/GroupMain/etc.png"
import { memberStore } from "../../stores/ModalStore";
import { Link, useParams } from "react-router-dom";
import { getPartyDetail, getPartyThumb } from "../../api/party";
import { PartyDetailData, MemberData } from "../../types/GroupType";
import { userStore } from "../../stores/UserStore";
import GroupThumbNail from "./GropThumbNail";
import logo from '../../../icons/512.png'
import MiniImg from "./MiniImg";

const GroupProfile: React.FC = () => { 
  const { setMembers, setMemberOpen } = memberStore()
  const { groupKey } = userStore();
  const { groupPk } = useParams();
  const [groupInfo, setGroupInfo] = useState<PartyDetailData>({partyMembers: [], partyName: '', rootDirId: '', thumbnail: ''})
  useEffect(() => {
    getDetail()
  }, [])

  const getDetail = async () => {
    await getPartyDetail(Number(groupPk),
    (res) => {
      const data = res.data.data;
      res.data.data.partyMembers.forEach((item) => {
        if (item.type === 'S3') {
          const key = groupKey[Number(groupPk)];
          getImg(item, key)
        }
      });
      setGroupInfo({
        partyMembers: data.partyMembers,
        partyName: data.partyName,
        rootDirId: data.rootDirId,
        thumbnail: data.thumbnail ? data.thumbnail : logo })
      setMembers(data.partyMembers)
    },
    (err) => {
      console.error(err)
    })
  }

  
  const getImg = async (data:MemberData, key:string) => {
		const opt = {
			"x-amz-server-side-encryption-customer-key": key,
		};
		await getPartyThumb(
			data.profileUrl,
			opt,
      (res) => {
        const blob = new Blob([res.data], {type: 'image/png'})
        data.profileUrl = (URL.createObjectURL(blob))
      },
			(err) => { console.log(err); },
		);
	};

  const memberThumb = () => {
    const memberThumbs = groupInfo.partyMembers.map((data) => {
      if (data.type === 'S3') {
        return <MiniImg url={data.profileUrl} />
      } else {
        return <img key={data.memberId} src={data.profileUrl} className="rounded-full object-cover w-8 h-8 border-white border-2 -mr-2"/>
      }
    });
    return memberThumbs;
  }

  return (
    <div className="flex flex-col justify-center items-center gap-4">
      <div className="w-[90px] h-[90px] rounded-full border-disabled-gray border-[1px] flex justify-center items-center">
        <GroupThumbNail url={groupInfo.thumbnail} />
      </div>

      <div className="flex items-center gap-3">
        <div className="text-black text-xl font-gtr-B">{groupInfo.partyName}</div>
        <Link to={`/group/${groupPk}/config`}>
          <img src={settings} className="w-5 top-[1px]" />
        </Link>
      </div>

      <div className="flex" onClick={() => setMemberOpen()}>
        {memberThumb()}
        <div className="w-8 h-8 rounded-full bg-gray flex justify-center items-center border-white border-2">
          <img src={etc} className="w-[13px]" />
        </div>
      </div>
    </div>
  )
};

export default GroupProfile;