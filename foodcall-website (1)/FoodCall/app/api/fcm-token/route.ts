import {db,validFamily,reply,event,input} from '../shared';

/** Registers the Android app's rotating FCM token against the website device. */
export async function POST(req:Request){
 const p=await input(req);
 const name=String(p.name||'').trim().slice(0,40);
 const device=String(p.device||'');
 const token=String(p.token||'');
 if(!validFamily(p.family)||!name||!device||device.length>100||token.length<20||token.length>4096)return reply({error:'Ungültiges Gerät'},400);
 try{
  const old=await db().prepare('SELECT fcm_token FROM devices WHERE family=? AND device=?').bind(p.family,device).first<{fcm_token:string|null}>();
  await db().prepare('INSERT INTO devices(family,device,name,last_seen,fcm_token) VALUES (?,?,?,?,?) ON CONFLICT(family,device) DO UPDATE SET name=excluded.name,last_seen=excluded.last_seen,fcm_token=excluded.fcm_token')
   .bind(p.family,device,name,Date.now(),token).run();
  if(old?.fcm_token!==token)await event(p.family,'push_connect',`${name} für Push registriert`);
  return reply({ok:true});
 }catch{return reply({error:'Push-Registrierung fehlgeschlagen'},503)}
}
