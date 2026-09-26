import {env} from 'cloudflare:workers';
import {db} from './shared';

type ServiceAccount={client_email:string;private_key:string;project_id:string;token_uri?:string};
let accessToken:string|undefined;
let accessTokenExpiresAt=0;

const base64Url=(value:Uint8Array|string)=>{
 const bytes=typeof value==='string'?new TextEncoder().encode(value):value;
 let binary=''; for(const byte of bytes)binary+=String.fromCharCode(byte);
 return btoa(binary).replace(/\+/g,'-').replace(/\//g,'_').replace(/=+$/,'');
};

function pemBytes(pem:string){
 const body=pem.replace(/-----BEGIN PRIVATE KEY-----|-----END PRIVATE KEY-----|\s/g,'');
 const binary=atob(body); return Uint8Array.from(binary,c=>c.charCodeAt(0));
}

async function oauthToken(service:ServiceAccount){
 if(accessToken&&Date.now()<accessTokenExpiresAt)return accessToken;
 const now=Math.floor(Date.now()/1000);
 const header=base64Url(JSON.stringify({alg:'RS256',typ:'JWT'}));
 const claims=base64Url(JSON.stringify({iss:service.client_email,scope:'https://www.googleapis.com/auth/firebase.messaging',aud:service.token_uri||'https://oauth2.googleapis.com/token',iat:now,exp:now+3600}));
 const signingInput=`${header}.${claims}`;
 const key=await crypto.subtle.importKey('pkcs8',pemBytes(service.private_key),{name:'RSASSA-PKCS1-v1_5',hash:'SHA-256'},false,['sign']);
 const signature=base64Url(new Uint8Array(await crypto.subtle.sign('RSASSA-PKCS1-v1_5',key,new TextEncoder().encode(signingInput))));
 const response=await fetch(service.token_uri||'https://oauth2.googleapis.com/token',{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},body:new URLSearchParams({grant_type:'urn:ietf:params:oauth:grant-type:jwt-bearer',assertion:`${signingInput}.${signature}`})});
 if(!response.ok)throw new Error('Firebase-Anmeldung fehlgeschlagen');
 const body=await response.json() as {access_token:string;expires_in:number};
 accessToken=body.access_token; accessTokenExpiresAt=Date.now()+Math.max(60,body.expires_in-60)*1000;
 return accessToken;
}

async function fcmRequest(token:string,data:Record<string,string>,service:ServiceAccount){
 const bearer=await oauthToken(service);
 return fetch(`https://fcm.googleapis.com/v1/projects/${service.project_id}/messages:send`,{method:'POST',headers:{Authorization:`Bearer ${bearer}`,'Content-Type':'application/json'},body:JSON.stringify({message:{token,data,android:{priority:'high',direct_boot_ok:true}}})});
}

function serviceAccount(){
 const raw=(env as Cloudflare.Env).FCM_SERVICE_ACCOUNT_JSON;
 if(!raw)return null;
 let service:ServiceAccount;
 try{service=JSON.parse(raw) as ServiceAccount;if(!service.client_email||!service.private_key||!service.project_id)throw new Error('invalid');return service;}catch{return null;}
}

export async function sendFoodReady(family:string,alarmId:string,meal:string){
 const service=serviceAccount();
 if(!service)return {sent:0,failed:0}; // The call itself still works while the secret is being configured.
 const devices=await db().prepare('SELECT fcm_token FROM devices WHERE family=? AND fcm_token IS NOT NULL').bind(family).all<{fcm_token:string}>();
 const results=await Promise.allSettled(devices.results.map(row=>fcmRequest(row.fcm_token,{type:'FOOD_READY',alarmId,meal},service)));
 let sent=0,failed=0;
 for(const result of results){if(result.status==='fulfilled'&&result.value.ok)sent++;else failed++;}
 return {sent,failed};
}

/** Ends the native attention signal on the device that chose a later arrival time. */
export async function sendFoodClear(family:string,device:string,alarmId:string){
 const service=serviceAccount(); if(!service)return;
 const row=await db().prepare('SELECT fcm_token FROM devices WHERE family=? AND device=?').bind(family,device).first<{fcm_token:string|null}>();
 if(!row?.fcm_token)return;
 await fcmRequest(row.fcm_token,{type:'FOOD_CLEAR',alarmId},service);
}
