# tzbp-be

The backend API for the tzbp solution. API is as follows:

**Authentication**: For demo purposes, all API requests require a header with the name `user` to be set. Pick whatever name you like, if you want to receive notifications make sure it's a valid email address.

See all accepted temporary no parking zones (use POST to request one):
`GET https://1e31fd74.ngrok.io/api/zones`

See your zones: `GET https://1e31fd74.ngrok.io/api/zones`

See your subscriptions (use POST to create one): `GET https://1e31fd74.ngrok.io/api/subscriptions`

## Development

Running locally: `./gradlew run` - no prerequisites required

## External dependencies / libraries

See [`build.gradle`](build.gradle)

- `ktor` is our http server
- `jackson` is for working with json
- `locationtech / noggit` is for working with geojson (mainly calculating intersects for the subscription feature)
- `simplejavamail` - you guessed it, sending mails! (again for subscription)

