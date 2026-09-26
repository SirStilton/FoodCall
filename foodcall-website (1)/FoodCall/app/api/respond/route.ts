import {db,validFamily,reply,event,input} from '../shared';
import {sendFoodClear} from '../fcm';
export async function POST(req:Request){
 const p=await input(req);const name=String(p.name||'').trim().slice(0,40),device=String(p.device||'');
 if(!validFamily(p.family)||!name||device.length>100||!device||![0,1,5].includes(Number(p.minutes))||typeof p.callId!=='string')return reply({error:'Ungültige Antwort'},400);
 try{const call=await db().prepare('SELECT id FROM calls WHERE id=? AND family=?').bind(p.callId,p.family).first();if(!call)return reply({error:'Call nicht gefunden'},404);
 await db().prepare('INSERT INTO responses(call_id,device,name,minutes,created) VALUES (?,?,?,?,?) ON CONFLICT(call_id,device) DO UPDATE SET name=excluded.name,minutes=excluded.minutes,created=excluded.created').bind(p.callId,device,name,p.minutes,Date.now()).run();await event(p.family,'response',`${name} → ${Number(p.minutes)===0?'Sofort':`${p.minutes} min`}`);
 if(Number(p.minutes)!==0)await sendFoodClear(p.family,device,p.callId);
 return reply({ok:true})}catch{return reply({error:'Antwort fehlgeschlagen'},503)}
}
