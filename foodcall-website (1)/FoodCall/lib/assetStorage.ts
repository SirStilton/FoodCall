export const keys=['logo','client-paper-bg','client-lights-bg','caller-wood-bg','breakfast-icon','lunch-icon','dinner-icon','snack-icon','dev-options-icon'] as const;
export type AssetKey=typeof keys[number];
const filenames:Record<string,AssetKey>={
 'fork & plate fc logo.png':'logo','sunlit leaf shadows on cream.png':'client-paper-bg','dreamy golden bokeh interior.png':'client-lights-bg','warm natural oak wood grain.png':'caller-wood-bg',
 'breakfast.png':'breakfast-icon','lunch.png':'lunch-icon','dinner.png':'dinner-icon','snack.png':'snack-icon','dev-options.png':'dev-options-icon'
};
const defaults:Record<AssetKey,string>={logo:'/assets/logo.png','client-paper-bg':'/assets/paper.png','client-lights-bg':'/assets/lights.png','caller-wood-bg':'/assets/wood.png','breakfast-icon':'/assets/breakfast.png','lunch-icon':'/assets/lunch.png','dinner-icon':'/assets/dinner.png','snack-icon':'/assets/snack.png','dev-options-icon':'/assets/dev.png'};
function open(){return new Promise<IDBDatabase>((resolve,reject)=>{const req=indexedDB.open('foodcall-assets',1);req.onupgradeneeded=()=>req.result.createObjectStore('images');req.onsuccess=()=>resolve(req.result);req.onerror=()=>reject(req.error)})}
async function tx<T>(mode:IDBTransactionMode,act:(store:IDBObjectStore)=>IDBRequest<T>){const db=await open();return new Promise<T>((resolve,reject)=>{const transaction=db.transaction('images',mode);const req=act(transaction.objectStore('images'));req.onsuccess=()=>resolve(req.result);req.onerror=()=>reject(req.error);transaction.oncomplete=()=>db.close();transaction.onerror=()=>reject(transaction.error)})}
export const saveAsset=(name:AssetKey,blob:Blob)=>tx('readwrite',s=>s.put(blob,name));
export const getAsset=(name:AssetKey)=>tx<Blob|undefined>('readonly',s=>s.get(name));
export const hasAsset=async(name:AssetKey)=>Boolean(await getAsset(name));
export const hasAllAssets=async()=>{for(const key of keys)if(!await hasAsset(key))return false;return true};
export const clearAssets=()=>tx('readwrite',s=>s.clear());
const urls=new Map<string,string>();
export async function getAssetUrl(name:AssetKey){if(urls.has(name))return urls.get(name)!;const blob=await getAsset(name);const url=blob?URL.createObjectURL(blob):defaults[name];urls.set(name,url);return url}
export function resetAssetUrls(){for(const url of urls.values())if(url.startsWith('blob:'))URL.revokeObjectURL(url);urls.clear()}
export async function importAssets(files:FileList|File[]){const found=new Map<AssetKey,File>();for(const file of Array.from(files)){const key=filenames[file.name.trim().toLowerCase()];if(key&&file.type.startsWith('image/'))found.set(key,file)}for(const [key,file] of found)await saveAsset(key,file);resetAssetUrls();return {found:[...found.keys()],missing:keys.filter(k=>!found.has(k))}}
export async function useIncludedAssets(){for(const key of keys){const response=await fetch(defaults[key]);if(!response.ok)throw new Error('Vorlagen konnten nicht geladen werden');await saveAsset(key,await response.blob())}resetAssetUrls()}
