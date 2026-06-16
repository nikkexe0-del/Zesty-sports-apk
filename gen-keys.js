import { execSync } from 'child_process';
import { readFileSync } from 'fs';
try {
  execSync('keytool -genkey -v -keystore release.keystore -alias release -keyalg RSA -keysize 2048 -validity 10000 -storepass "password" -keypass "password" -dname "CN=zestyysports, OU=app, O=zesty, L=City, S=State, C=US"');
  const fileContent = readFileSync('release.keystore');
  console.log(fileContent.toString('base64'));
} catch (e) {
  console.error(e);
}
