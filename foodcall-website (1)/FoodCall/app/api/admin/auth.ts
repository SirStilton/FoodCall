import {env} from 'cloudflare:workers';
import {db,event} from '../shared';

const secret=()=> (env as unknown as Record<string,string>).FOODCALL_ADMIN_SESSION_KEY;
const password=()=> (env as unknown as Record<string,string>).FOODCALL_ADMIN_PASSWORD;
const bytes=(s:string)=>new TextEncoder().encode(s);
async function signature(value:string){const key=await crypto.subtle.importKey('raw',bytes(secret()),{name:'HMAC',hash:'SHA-256'},false,['sign']);const result=await crypto.subtle.sign('HMAC',key,bytes(value));return [...new Uint8Array(result)].map(x=>x.toString(16).padStart(2,'0')).join('')}
export function validPassword(value:unknown){if(typeof value!=='string'||!password())return false;const a=bytes(value),b=bytes(password());let diff=a.length^b.length;for(let i=0;i<Math.max(a.length,b.length);i++)diff|=(a[i]||0)^(b[i]||0);return diff===0}
export async function adminCookie(){const expires=String(Date.now()+60*60*1000);return `fc-admin=${expires}.${await signature(expires)}; HttpOnly; Secure; SameSite=Lax; Path=/api; Max-Age=3600`}
export async function isAdmin(req:Request){if(!secret())return false;const value=req.headers.get('cookie')?.match(/(?:^|;\s*)fc-admin=([^;]+)/)?.[1]||'';const [expires,sig]=value.split('.');if(!/^\d{13}$/.test(expires||'')||Number(expires)<Date.now()||!sig||!/^[a-f0-9]{64}$/.test(sig))return false;const expected=await signature(expires);let diff=0;for(let i=0;i<64;i++)diff|=sig.charCodeAt(i)^expected.charCodeAt(i);return diff===0}
export const forbidden=()=>Response.json({error:'Admin-Anmeldung erforderlich'},{status:401,headers:{'Cache-Control':'no-store'}});
export async function loginBucket(req:Request){
 const ip=req.headers.get('CF-Connecting-IP')||'unknown';
 const key=await crypto.subtle.importKey('raw',bytes(secret()),{name:'HMAC',hash:'SHA-256'},false,['sign']);
 const digest=await crypto.subtle.sign('HMAC',key,bytes('admin-login:'+ip));
 return 'admin-login:'+[...new Uint8Array(digest)].map(x=>x.toString(16).padStart(2,'0')).join('');
}
export async function failedLoginCount(bucket:string){
 const row=await db().prepare("SELECT COUNT(*) AS count FROM events WHERE family=? AND kind='login-failed' AND created>?").bind(bucket,Date.now()-15*60_000).first<{count:number}>();
 return row?.count||0;
}
export async function recordFailedLogin(bucket:string){await event(bucket,'login-failed','Failed admin login')}
