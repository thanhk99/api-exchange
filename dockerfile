# =========================================
# BUILD STAGE: Biên dịch và đóng gói ứng dụng
# =========================================
FROM eclipse-temurin:21-jdk-jammy as builder

WORKDIR /workspace/app

# Copy các file cấu hình Maven trước để tận dụng Docker cache
COPY mvnw .
COPY .mvn .mvn
RUN chmod +x mvnw
COPY pom.xml .

# Download dependencies trước (tận dụng cache layer)
RUN ./mvnw dependency:go-offline

# Copy source code
COPY src src

# === SỬA 1: Bỏ comment dòng này để build ứng dụng ===
# Build ứng dụng và bỏ qua tests
RUN ./mvnw clean package -DskipTests

# Tách file JAR thành các thư mục riêng để tối ưu
RUN mkdir -p target/dependency && (cd target/dependency; jar -xf ../*.jar)

# =========================================
# RUNTIME STAGE: Stage để chạy ứng dụng
# =========================================
FROM eclipse-temurin:21-jre-jammy

# Thư mục làm việc
WORKDIR /app

# Thêm volume cho thư mục tạm (quan trọng cho Spring Boot)
VOLUME /tmp

# Sao chép các phần cần thiết từ stage builder
ARG DEPENDENCY=/workspace/app/target/dependency
COPY --from=builder ${DEPENDENCY}/BOOT-INF/lib /app/lib
COPY --from=builder ${DEPENDENCY}/META-INF /app/META-INF
COPY --from=builder ${DEPENDENCY}/BOOT-INF/classes /app

# === SỬA 2: Cấu hình cổng ứng dụng sẽ chạy ===
# Render sẽ sử dụng cổng này. Đảm bảo ứng dụng của bạn cũng chạy trên cổng này.
# Bạn có thể cấu hình trong Render Environment Variables: SERVER_PORT=8000
EXPOSE 8000

# === SỬA 3: Sử dụng JarLauncher của Spring Boot để khởi chạy ===
# Đây là cách khởi chạy tiêu chuẩn cho một ứng dụng Spring Boot đã được giải nén.
ENTRYPOINT ["java", \
            "-cp", ".:app/lib/*", \
            "-XX:+UseContainerSupport", \
            "-XX:MaxRAMPercentage=75.0", \
            "org.springframework.boot.loader.launch.JarLauncher"]