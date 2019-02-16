# Roughly what the database will look like

Two [collections](https://firebase.google.com/docs/firestore/data-model#collections):
 - Patients
 - Nurses
 

## The Patients Collection
Contains a bunch of `patient` [documents](https://firebase.google.com/docs/firestore/data-model#documents) that look like this:
```typescript
{
	name: string
	email: string
	nurseID: string
}
```

Each `patient` document also has a `videos` collection. The documents in the `videos` collection look like this: 
```typescript
{
	videoURL: string
	thumbnailURL: string
	width: number
	height: number
	submittedOn: number
	markedForManualReview: boolean
}
```

## The Nurses Collection
Contains a bunch of documents that look like this:
```typescript
{

	name: string
	email: string
	
	// Also maybe these:
	phoneNumber: string
	hospitalAddress: string
	workHours
	
}
``` 



