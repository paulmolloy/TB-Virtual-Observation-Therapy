# Using quick, unsafe links
These links are not safe because anyone on the internet can use them to add as many users as they want. For now though to add users for testing purposes:

Add a nurse:
```
http://34.73.42.71:8080/api/insecure/add-nurse?name=Bob&email=bob@email.com&password=Password123
(Don't forget to change "Bob", "bob@email.com" and "Password123")
```

Add a patient:
```
http://34.73.42.71:8080/api/insecure/add-patient?name=Bob&email=bob@email.com&password=Password123&nurseID=abcde123
(Don't forget to change "Bob", "bob@email.com", "Password123" and nurseID)
```

# Using POST requests
In order to use these links we first need to get [Auth Tokens](https://firebase.google.com/docs/auth/admin/verify-id-tokens) working on the front end

## Adding a nurse
Send a JSON in a POST request to `http://34.73.42.71:8080/api/nurses` that looks like this:
```typescript
{
    name: string
    email: string
    password: string
    authToken: string
}
```

## Adding a patient
Send a JSON in a POST request to `http://34.73.42.71:8080/api/patients` that looks like this:
```typescript
{
    name: string
    email: string
    password: string
    nurseID: string
    authToken: string
}
```