import { AxiosResponse } from 'axios';
import { privateApi } from '../utils/http-commons';
import { ResponseData } from '../types/ApiResponseType';
import { AlbumResponse, DirInfoType, delFilesDtoType, getChildDirResponseType, getDetailImgType, handleInfoType, patchMoveDirDtoType, sseKeyDtoType, thumbNailResponseType } from '../types/AlbumType';

const url = 'directories';

export const getDirectory = async (
  dirId: string,
  Response : (Response : AxiosResponse<AlbumResponse>) => void, 
  Error : (Error : AxiosResponse<ResponseData>) => void) => {
  await privateApi.get(`/${url}/${dirId}`)
  .then(Response)
  .catch(Error)
}

export const createDirectory = async (
  dirInfo : DirInfoType,
  Response : (Response : AxiosResponse<AlbumResponse>) => void, 
  Error : (Error : AxiosResponse<ResponseData>) => void) => {
  await privateApi.post(`/${url}`, dirInfo)
  .then(Response)
  .catch(Error)
}

export const delDirectory = async (
  dirId: string,
  sseKey: sseKeyDtoType,
  Response : (Response : AxiosResponse<AlbumResponse>) => void, 
  Error : (Error : AxiosResponse<ResponseData>) => void) => {
  await privateApi.delete(`/${url}/${dirId}`, sseKey)
  .then(Response)
  .catch(Error)
}

export const editDirectory = async (
  dirId : string,
  newName: string,
  Response : (Response : AxiosResponse<AlbumResponse>) => void, 
  Error : (Error : AxiosResponse<ResponseData>) => void) => {
  await privateApi.patch(`/${url}/${dirId}/rename`, { 'newName': newName })
  .then(Response)
  .catch(Error)
}

export const getDetailImg = async (
  dirId : string,
  fileId: string,
  Response : (Response : AxiosResponse<getDetailImgType>) => void, 
  Error : (Error : AxiosResponse<ResponseData>) => void) => {
  await privateApi.get(`/${url}/${dirId}/files/${fileId}`)
  .then(Response)
  .catch(Error)
}

export const getThumbnailNav = async (
  dirId : string,
  Response : (Response : AxiosResponse<thumbNailResponseType>) => void, 
  Error : (Error : AxiosResponse<ResponseData>) => void) => {
  await privateApi.get(`/${url}/${dirId}/thumbnail2`)
  .then(Response)
  .catch(Error)
}

export const delFiles = async (
  dirId : string,
  delFilesDto: delFilesDtoType,
  Response : (Response : AxiosResponse<thumbNailResponseType>) => void, 
  Error : (Error : AxiosResponse<ResponseData>) => void) => {
  await privateApi.delete(`/${url}/${dirId}/files`, delFilesDto)
  .then(Response)
  .catch(Error)
}

export const getDirStructure = async (
  dirId : string,
  Response : (Response : AxiosResponse<getChildDirResponseType>) => void, 
  Error : (Error : AxiosResponse<ResponseData>) => void) => {
  await privateApi.get(`/${url}/${dirId}/structure`)
  .then(Response)
  .catch(Error)
}

export const moveFiles = async (
  dirId : string,
  moveInfo: handleInfoType,
  Response : (Response : AxiosResponse<getChildDirResponseType>) => void, 
  Error : (Error : AxiosResponse<ResponseData>) => void) => {
  await privateApi.patch(`/${url}/${dirId}/files/move`, moveInfo)
  .then(Response)
  .catch(Error)
}

export const copyFiles = async (
  dirId : string,
  copyInfo: handleInfoType,
  Response : (Response : AxiosResponse<getChildDirResponseType>) => void, 
  Error : (Error : AxiosResponse<ResponseData>) => void) => {
  await privateApi.patch(`/${url}/${dirId}/files/copy`, copyInfo)
  .then(Response)
  .catch(Error)
}


export const moveDirectory = async (
  dirId: string,
  patchMoveDirDto : patchMoveDirDtoType,
  Response : (Response : AxiosResponse<AlbumResponse>) => void, 
  Error : (Error : AxiosResponse<ResponseData>) => void) => {
  await privateApi.patch(`/${url}/${dirId}`, patchMoveDirDto)
  .then(Response)
  .catch(Error)
}