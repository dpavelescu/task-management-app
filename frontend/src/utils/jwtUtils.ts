interface JwtPayload {
  exp: number;
  sub: string;
  // Add other JWT claims as needed
}

export function parseJwt(token: string): JwtPayload | null {
  try {
    const base64Url = token.split('.')[1];
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split('')
        .map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    );

    return JSON.parse(jsonPayload);
  } catch (error) {
    console.error('Failed to parse JWT:', error);
    return null;
  }
}

export function isTokenExpired(token: string | null): boolean {
  if (!token) return true;
  
  try {
    const payload = parseJwt(token);
    if (!payload) return true;
    
    const currentTime = Math.floor(Date.now() / 1000); // Convert to seconds
    return payload.exp < currentTime;
  } catch (error) {
    console.error('Error checking token expiration:', error);
    return true;
  }
}
