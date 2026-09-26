import {isAdmin} from '../auth';
export async function GET(req:Request){return Response.json({authenticated:await isAdmin(req)},{headers:{'Cache-Control':'no-store'}})}
