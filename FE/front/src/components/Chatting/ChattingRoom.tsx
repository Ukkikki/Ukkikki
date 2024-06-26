import React, { useEffect, useRef } from "react";
import { ChattingRoomProps } from "../../types/ChatType";
import { useStore } from "zustand";
import { lastStore, loadingStore } from "../../stores/ChatStore";
import LoadingGif from "../@commons/LoadingGif";
import ChatUserThumb from "./ChatUserThumb";

const ChattingRoom: React.FC<ChattingRoomProps> = ({ msgList }) => {
  const { isLoading } = useStore(loadingStore)
  const { isLast } = useStore(lastStore)


  const dateHandler = (dateInfo: string) => {
    const date = new Date(dateInfo)
    
    return date.toLocaleDateString('ko-KR', {
      year: '2-digit',
      month: '2-digit',
      day: '2-digit',
    }).replace(/\. /g, '.').replace(/\.$/, '');
  } 

  const timeHandler = (dateInfo: string) => {
    const date = new Date(dateInfo)
    
    return date.toLocaleTimeString('ko-KR', {
      hour: '2-digit',
      minute: '2-digit',
    });
  } 

  const chatContainerRef = useRef<HTMLDivElement>(null);

  // 채팅의 가장 마지막 메시지를 가장 처음 볼 수 있도록 구현
  useEffect(() => {
    if (chatContainerRef.current) {
      chatContainerRef.current.scrollTop = chatContainerRef.current.scrollHeight;
    }
  }, [msgList]);

  return (
    <div ref={chatContainerRef} className="flex flex-col w-full h-full overflow-scroll scrollbar-hide z-10">
      <div id='observer' className="h-[5px] mb-2 w-full flex justify-center">
        {(isLoading && !isLast) && <LoadingGif /> }
      </div>
      { msgList.map((item, idx) => (
        item.chatType === 'ENTER' 
        ? 
        <div key={idx} className="w-full mb-[10px] rounded-[15px] py-2 bg-disabled-gray opacity-80 font-pre-R text-white text-sm flex justify-center items-center">
          {item.memberName} 님이 참여했습니다.
        </div>
        : item.chatType === 'EXIT' ?
        <div key={idx} className="w-full mb-[10px] rounded-[15px] py-2 bg-disabled-gray opacity-80 font-pre-R text-white text-sm flex justify-center items-center">
          {item.memberName} 님이 퇴장했습니다.
        </div>
        :
        <div key={idx} className="w-full mb-[10px] rounded-[15px] bg-soft-gray opacity-80 flex py-2 px-[10px] gap-[10px]">
          {item.profileType === 'S3' ?
            <ChatUserThumb url={item.profileUrl} /> 
            : 
            <img src={item.profileUrl} className="rounded-full w-[50px] h-[50px]" />
          }
          <div className="flex flex-col w-[calc(100%-60px)] gap-2">
            <div className="flex gap-2 items-center mt-[2px]">
              <div className="font-pre-SB text-black text-xs">{item.memberName}</div>
              <div className="font-pre-L text-point-gray text-[10px]">{dateHandler(item.createDate)} &nbsp; {timeHandler(item.createDate)}</div>
            </div>
            <div className="font-pre-L text-black text-sm">{item.content}</div>
          </div>
        </div>
      ))}
    </div>
  )
};

export default ChattingRoom;