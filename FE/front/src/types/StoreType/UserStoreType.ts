export interface UserStoreType {
  isLogin: boolean;
  isInsertPass : boolean;
  isCheckPass : boolean;

  accessToken: string;
  userId : string;
  userName : string;
  userProfile : string;
  groupKey : Record<number, string>;
  uploadGroupId: number|null;
  simplePass : string;

  setIsLogin: (newBool: boolean) => void;
  setIsInsert: (newBool: boolean) => void;
  setIsCheck: (newBool: boolean) => void;

  setAccessToken : (newToken: string) => void;
  setUserId : (newId: string) => void;
  setUserName : (newName: string) => void;
  setUserProfile : (newUrl: string) => void;
  setGroupKey: (newList: Record<number, string>) => void;
  setUploadGroupId: (newId: number|null) => void;
  setSimplePass: (newPass: string) => void;
}