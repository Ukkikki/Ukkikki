import React, { useEffect, useState } from "react";
import folder from "@/assets/Album/folder.png"
import { useParams } from "react-router-dom";
import { useStore } from "zustand";
import { selectModeStore } from "../../stores/AlbumStore";
import { getTrashBin } from "../../api/trash-bin";
import { TrashItemType } from "../../types/TrashType";
import SecureImg from "../Album/SecureImg";
import TrashSelectModeImg from "./TrashSelectModeImg";
import { updateTrashStore } from "../../stores/TrashStore";

const TrashMain: React.FC = () => {
  const { selectMode } = useStore(selectModeStore)
  const [trashList, setTrashList] = useState<TrashItemType[]>([])
  const { needUpdate } = useStore(updateTrashStore)
  
  const { groupPk } = useParams();

  useEffect(() => {
    getTrashBinHandler()
  }, [groupPk, needUpdate])

  const getTrashBinHandler = async () => {
    await getTrashBin(
      Number(groupPk),
      (res) => {
        setTrashList(res.data.data)
      },
      (err) => { console.error(err) }
    )
  }

  const dateHandler = (dateString: string) => {
    if (!dateString) return "";

    const [year, month, day] = dateString.split('-');
    return `${year.slice(2)}.${month}.${day}`;
  };
  
  return (
    <div>
      <div className="px-4 mt-2 mb-4 rounded-xl py-2 font-pre-R text-red text-base mx-4 bg-soft-gray ">폴더는 단일 선택만 가능합니다</div>
      <div className="grid grid-cols-3 sm:grid-cols-5 md:grid-cols-6 lg:grid-cols-8 xl:grid-cols-10 2xl:grid-cols-12 px-4 gap-1 gap-y-5 overflow-scroll scrollbar-hide ">
        {trashList!.map((item, idx) => (
          <div>
            {
              selectMode ? 
                <TrashSelectModeImg key={idx} item={item} />
              :
                item.type === 'DIRECTORY' ?
                  <div key={idx} className="flex flex-col justify-center items-center gap-1">
                    <img src={folder} className="w-[82px] h-[65px]" />
                    <div className="font-pre-R text-center text-xs">{item.name}</div>
                  </div>
                :
                  <div key={idx} className="flex flex-col justify-center items-center">
                    <SecureImg url={item.url} />
                    <div className="font-pre-L text-sm text-black">만료일 <span className="text-red">{dateHandler(item.deadLine)}</span></div>
                  </div>
            }
          </div>
        ))}
      </div>
    </div>
  )
};

export default TrashMain;