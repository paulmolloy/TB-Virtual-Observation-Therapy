# Firebase Functions
[Firebase Functions?](https://firebase.google.com/docs/functions)

## Setting up Functions on your computer
1. Install [Node.js](https://nodejs.org/)
2. Install the Firebase CLI:
```shell 
npm install -g firebase-tools
```
3. Login to Firebase CLI:
```shell
firebase login
```
4. Download the required dependencies:
```shell
cd blah/blah/blah/TB-Virtual-Observation-Therapy/Firebase/functions
npm install
```

## Deploying changes
1. Edit the required files in `blah/blah/blah/TB-Virtual-Observation-Therapy/Firebase/functions/src`
2. Upload the changes to Firebase servers:
```shell
cd blah/blah/blah/TB-Virtual-Observation-Therapy/Firebase/functions
npm run deploy
```