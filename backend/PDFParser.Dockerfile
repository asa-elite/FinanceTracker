FROM eclipse-temurin:21-jdk

RUN apt-get update && apt-get install -y \
    tesseract-ocr \
    tesseract-ocr-eng \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY . .

RUN chmod +x ./gradlew

CMD ["./gradlew", "runPdfParser"]