# Build stage
FROM node:20-alpine AS build
WORKDIR /app

# Copy package files first to cache npm install
COPY package*.json ./
RUN npm install

# Copy source code and build
COPY . .
RUN npm run build

# Runtime stage
FROM node:20-alpine
WORKDIR /app
COPY --from=build /app ./

EXPOSE 3000
CMD ["npm", "run", "start"]
