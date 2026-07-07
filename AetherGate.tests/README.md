docker volume create keycloak-data


docker run --rm -p 127.0.0.1:8080:8080 \
  -e KC_BOOTSTRAP_ADMIN_USERNAME=admin \
  -e KC_BOOTSTRAP_ADMIN_PASSWORD=admin \
  -v keycloak-data:/opt/keycloak/data \
  quay.io/keycloak/keycloak start-dev

