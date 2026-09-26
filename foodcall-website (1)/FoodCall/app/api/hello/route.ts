import {db,validFamily,reply,event,input} from '../shared';
export async function POST(req:Request){
 const p=await input(req);const name=String(p.name||'').trim().slice(0,40),device=String(p.device||'');
 if(!validFamily(p.family)||!name||!device||device.length>100)return reply({error:'Ungültiges Gerät'},400);
 try{const old=await db().prepare('SELECT device FROM devices WHERE family=? AND device=?').bind(p.family,device).first();await db().prepare('INSERT INTO devices(family,device,name,last_seen) VALUES (?,?,?,?) ON CONFLICT(family,device) DO UPDATE SET name=excluded.name,last_seen=excluded.last_seen').bind(p.family,device,name,Date.now()).run();if(!old)await event(p.family,'connect',`${name} verbunden`);return reply({ok:true})}catch{return reply({error:'Verbindung fehlgeschlagen'},503)}
}
