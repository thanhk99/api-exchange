#Build stage 
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

# Build ứng dụng và bỏ qua tests
# RUN ./mvnw clean package -DskipTests

# Tách file JAR thành các thư mục riêng để tối ưu
RUN mkdir -p target/dependency && (cd target/dependency; jar -xf ../*.jar)

# Runtime stage - chỉ sử dụng JRE để giảm kích thước
FROM eclipse-temurin:21-jre-jammy

# Thư mục làm việc
WORKDIR /app

# Thêm volume cho thư mục tạm
VOLUME /tmp

# Sao chép các phần cần thiết từ stage builder
ARG DEPENDENCY=/workspace/app/target/dependency
COPY --from=builder ${DEPENDENCY}/BOOT-INF/lib /app/lib
COPY --from=builder ${DEPENDENCY}/META-INF /app/META-INF
COPY --from=builder ${DEPENDENCY}/BOOT-INF/classes /app

# Thiết lập biến môi trường (nếu cần)
# ENV SPRING_PROFILES_ACTIVE=prod

# Port ứng dụng sẽ chạy
EXPOSE 8000

# Lệnh khởi chạy ứng dụng
ENTRYPOINT ["java", \
            "-cp", "app:app/lib/*", \
            "-XX:+UseContainerSupport", \
            "-XX:MaxRAMPercentage=75.0", \
            "-XX:InitialRAMPercentage=50.0", \
            "-XX:MinRAMPercentage=50.0", \
            "api.exchange.ExchangeApplication"]