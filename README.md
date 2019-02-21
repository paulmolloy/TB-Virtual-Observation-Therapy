# TB-Virtual-Observation-Therapy
Add your name when you've been able to run the android app locally.
Paul
Neasa
Edvardas
Aoife
Dan

# Database
See roughly what the database looks like [here](./Documentation/DatabaseSpec.md)

## Adding new users
These links are not safe because anyone on the internet can use them to add as many users as they want. For now though to add users for testing purposes:

Add a patient:
```
https://us-central1-tb-vot.cloudfunctions.net/addPatient?name=Bob&email=bob@email.com&password=Password123&nurseID=abcde123
(Don't forget to change "Bob", "bob@email.com", "Password123" and nurseID)
```

Add a nurse:
```
https://us-central1-tb-vot.cloudfunctions.net/addNurse?name=Bob&email=bob@email.com&password=Password123
(Don't forget to change "Bob", "bob@email.com" and "Password123")
```
