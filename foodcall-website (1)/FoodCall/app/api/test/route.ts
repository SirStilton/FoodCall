import {validFamily,reply,event,input} from '../shared';
import {isAdmin,forbidden} from '../admin/auth';
export async function POST(req:Request){if(!await isAdmin(req))return forbidden();const p=await input(req);if(!validFamily(p.family))return reply({error:'Ungültige Familie'},400);try{await event(p.family,'test','Alle Geräte testen');return reply({ok:true})}catch{return reply({error:'Test fehlgeschlagen'},503)}}
