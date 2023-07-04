# Add site

Adding a site, or, as we call it colloquially: 'adding a bank', is one of the core functions of `site-management`.
This document is a detailed description of what happens both from the perspective of a [client](../concepts/client.md) and under the hood.


## Prerequisites

- the client (`C`) is an authenticated client, e.g.: the client can call the Yolt API
- the client has been onboarded with at least one site `S`
- there is an end user `U` that has an account at the financial institution represented by site `S`


## Happy flow

The below steps go through the normal happy flow for adding a bank in the case of a direct connection.
Furthermore, we only describe the 'desired' flow below, we don't treat the deprecated endpoints.
References to scraper providers are made, but we don't go into much detail.

More information on flows related to deprecated endpoints and scraping providers can be added in further chapters.

This diagram is an overview of the flow.

![add bank](https://git.yolt.io/pages/backend-tools/yolt-architecture-diagram/downloaded-other-diagrams/site-management_add-bank.svg?job=build)
[source](/diagrams/add-bank.puml)


### `GET /initiate-user-site`

**What the client needs to do**

The very first step is for `C` to `GET /{siteId}/initiate-user-site` where `{siteId}` is the uuid of `S`.
The client `C` needs to provide additional details in HTTP headers, namely:

- `clientId`: `C`'s own clientId
- `redirectUrlId`: the id of the redirect url that was configured when `C` onboarded with `S` (*not* required: in case of scraping providers)


**What happens in site-management**

- Check that adding a [user-site](../concepts/user-site.md) is allowed for the [site](../concepts/site.md), as follows:
  
  - if `S` has the flag `noLongerSupported` set to true: the site *cannot* be added
  - if `C` is internal client (Yolt): the site *can* be added. (_yes: this is a horrible hack_)
  - if `S` is not enabled for `C`: the site *cannot* be added
  - if the provider `P` that is linked to `S`, was not onboarded with the `redirectUrlId`, the site *cannot* be added
  - otherwise: the site *can* be added

- Create and store a new [user-site-session](../concepts/user-site-session.md)

  During the creation of the user-site-session the system generates:
   
   - `userSiteId`: a random uuid that will become the id for the [user-site](../concepts/user-site.md) that will be created later.
   - `stateId`: a random uuid that will be used during the oAuth2 flow, see also: [https://auth0.com/docs/protocols/oauth2/oauth-state#redirect-users](https://auth0.com/docs/protocols/oauth2/oauth-state#redirect-users)
   
  A [user-site-session](../concepts/user-site-session.md) has a default ttl of an hour.
  This means that after an hour without updates, user-site-session's are automatically deleted.

- Create and store a generated-session-state object

  We store:
  
  - `userSiteSessionId`, of the user-site-session that was just created
  - `stateId`, identical to the stateId stored in the user-site-session
  - `userSiteId`, the future id of the user-site to be created
  - `submitted`: a boolean set to false that indicates if the [step](../concepts/step.md) has been submitted
  
  This data is stored separately from the user-site-session and is kept indefinitely (until the user is deleted).
  We keep track of if and when the user gets back to us with the information required (either a redirect url or a filled in form)
  
- The code now branches based on if the provider is a scraper or a direct connection

  In the case of a scraper we return a [step](../concepts/step.md) that contains a form, this form needs to be filled in by the user and typically requires the user to enter credentials.
  
  In the case of a direct connection we return a [step](../concepts/step.md) that requires the user to visit a URL, that URL is a link to the banks webpage where the user can give their consent.
  The URL is constructed by the provider implementation that belongs to site `S`.
  The URL always contains a query parameter called `state` that contains `stateId`, the bank will pass this back to us.

**Note**: this endpoint must be called with method `GET`, but as can be seen, server side state is in fact changed, and the call it thus not idempotent.


### User action: complete the step

As mentioned above, the client `C` retrieves a 'step'.
That step is presented to the user by the client.

Let's assume the step requires the user to visit a url.
The user visits the url and provides their consent to the financial institution by filling in the fields.
After the user has finished giving their consent, the user is 'redirected' **by the bank** to the redirectUrl that is configured for `C` at the provider for `S`.

The client is then *responsible* for posting the **full URL** to us, this is treated in the next step.


### `POST /user-sites`

**What the client needs to do**

Client `C` needs to provide additional details in HTTP headers, namely: 

- clientId: `C`'s own clientId

In addition, the client needs to post an object, one of these:

- `UrlLogon`

  Contains 1 field, namely `redirectUrl` that should contain the full URL that the user was redirected to by the bank.

- `FormLogon` (a filled in form)
- `PolishApiUrlLogon` (we'll ignore this for now)

All of the above objects inherit from the `Logon` object, that contains a `userId`.

**What happens in site-management**

The method that does the interesting work here is the most complicated method in `site-management` by a large margin, this is it:
`nl.ing.lovebird.sitemanagement.usersite.CreateOrUpdateUserSiteService.processPostedLogin`

We only discuss what happens when the user posts a `UrlLogon` object, and if this is the first [step](../concepts/step.md).

- We correlate the request to a [user-site-session](../concepts/user-site-session.md) and perform administration

  These are the substeps:

  - `stateId` is extracted from the `redirectUrl` contained in the `UrlLogon` object 
  - based on the combination `userId` and `stateId` we retrieve the user-site-session from the database
  - we mark the `generated-session-state` object as 'submitted' and create a new generated-session-state object
  - we create a new `stateId` and store this in the user-site-session

- If the flag `noLongerSupported` is set to false for the site, abort processing.  This is very unlikely to happen in practice.

- Determine what situation we're in, this is the complex part:

  - if [user-site-session](../concepts/user-site-session.md).`operation` == `CREATE_USER_SITE` and there are no stored steps on the [user-site-session](../concepts/user-site-session.md)
    
    We know this is the very first `POST /user-sites` and we will create the [user-site](../concepts/user-site.md), this involves:
    
    - saving the [user-site](../concepts/user-site.md) to the database
    - some auxiliary administration (storing ExternalConsent)
    - sending a `CreateUserSiteEvent` to the `user-site-events` topic (reason: creation)
    - getting the [access-means](../concepts/access-means.md) or a step from the [provider](../concepts/provider.md)
        - if the [provider](../concepts/provider.md) returns access means, then
            - assuming this succeeds, we store the [access-means](../concepts/access-means.md) at the user-site level
            - we update the [user-site](../concepts/user-site.md) and set the status to `LOGIN_SUCCEEDED`
            - we send an `UpdateUserSiteEvent` to the `user-site-events` topic (reason: status change)
            - we instruct the [provider](../concepts/provider.md) to [fetch data](refresh-user-site.md) for the newly created [user-site](../concepts/user-site.md)
        - otherwise, the [provider](../concepts/provider.md) has returned a [step](../concepts/step.md) in which case:
            - store the step in the [user-site-session](../concepts/user-site-session.md) so that `C` can retrieve it
            - update the status of the [user-site](../concepts/user-site.md) to `STEP_NEEDED`
            - we send an `UpdateUserSiteEvent` to the `user-site-events` topic (reason: status change) 
    
    We are done processing.

  - if there is a stored step on the [user-site-session](../concepts/user-site-session.md)
  
    We know this is a subsequent call to `POST /user-sites` and we will process the step.
    We will get back to this in the [optional subsequent step](#_optional_-client-action-post-user-sites)

  - omitted: there is another branch of code related to updating a [user-site](../concepts/user-site.md), this is treated in detail in [update-site](update-site.md)

In short, after process completes this is always the case:

- a [user-site](../concepts/user-site.md) has been created
- if the status of the [user-site](../concepts/user-site.md) is `STEP_NEEDED`, the user needs to do [additional processing](#_optional_-client-action-get-usersiteidstep)


### Handling downstream service being unavailable
Once `site-management` has acquired the [access-means](../concepts/access-means.md) for a [user-site](../concepts/user-site.md)
from providers, it instructs the [provider](../concepts/provider.md) to [fetch data](refresh-user-site.md).
This call is made synchronously so that `site-management` can tell clients if the activity of fetching data was started correctly.


### _Optional_ client action: `GET /{userSiteId}/step`

After perform the first [`POST /user-sites`](#post-user-sites), the [client](../concepts/client.md) needs to retrieve the [user-site](../concepts/user-site.md) to inspect the status.
If the status is `STEP_NEEDED`, the [client](../concepts/client.md) needs to perform this optional step.

**todo** document


### _Optional_ client action: `POST /user-sites`

**todo** document posting of next step


