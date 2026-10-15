FROM node:22-alpine AS build
WORKDIR /build
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
# Where the header's Grafana link points (baked in at build time).
ARG VITE_GRAFANA_URL=http://localhost:3000/d/jdd-assistant
ENV VITE_GRAFANA_URL=${VITE_GRAFANA_URL}
RUN npm run build

FROM nginx:1.27-alpine
COPY --from=build /build/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
