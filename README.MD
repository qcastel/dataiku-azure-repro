# Azure AD issue

Some groups can't be retrieved from an Azure AD user when using the Azure AD graph SDK.

This repo shows how we currently retrieve the user groups.

Any advices on why the current code is not retrieving all the groups is welcome.

## How to test

run the following command:

```commandline
java -jar target/demo-0.0.1-SNAPSHOT.jar ${tenandId} ${clientId} ${clientSecret} ${userEmail}
```

Example of output:

```
User found! displayName = Quentin Castel
Groups:
- rnd-team-iteam
- jond-test-group1
- rnd_contributor
```