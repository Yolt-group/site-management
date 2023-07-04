Secrets
=======

This document contains information about the secrets that this service uses.
The shell commands were written on MacOS so you might need to alter the `base64` calls, the flags are different on Linux.

You may assume that the paragraphs with **How to generate this secret?** always produce base64 encoded output that is ready to be pasted in k8s.


site-management / encryption-key
---

**Type**

32 hex-encoded random bytes.

**Spring property**

`accessMeans.encryptionKey`

**Getting the secret from k8s**

`$ kubectl get secret site-management -o json | jq -r '.data["encryption-key"]' | base64 -D`

**How to check if the currently configured secret is correct?**

`$ kubectl get secret site-management -o json | jq -r '.data["encryption-key"]' | base64 -D | wc -c`

Executing the above statement should result in `64`.

**How to generate this secret?**

`$ openssl rand 32 -hex | tr -d '\n' | base64`

Note the subtle removal of the newline.
