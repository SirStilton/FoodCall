export async function GET(request: Request) {
  return Response.redirect(new URL('/foodcall-website.zip', request.url), 302);
}
