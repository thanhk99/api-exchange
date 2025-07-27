## API Endpoints

### POST localhost:8000/api/v1/auth/signup
- **Mô tả**: Đăng ký 
- **Request**: 
    ```json
    {
    "username": "thanhdz",
    "password": "Thanh2004@",
    "email":"thanh1232004@gmail.com"
    }
- **Response**:
    ```json
    {
        "user": {
            "id": 1,
            "email": "thanh1232004@gmail.com",
            "username": "thanhdz"
        },
        "accessToken": "eyJhbGciOiJIUzUxMiJ9.eyJyb2xlcyI6IlVTRVIiLCJzdWIiOiJ0aGFuaDEyMzIwMDRAZ21haWwuY29tIiwiaWF0IjoxNzUwMzA2Mjk2LCJleHAiOjE3NTAzMDcxOTZ9.gDXyVFYUtfEm9iHvtjmtVKyenWcpEA50sXAbOaI0AKM0cZlN1K7SpT0BOeJM9o0IY3j2xVMAjfRxF78CVqSepw",
        "refreshToken": "56fc1eb0-6a8a-4e03-a97a-4b0593968b7b",
        "message": "Đăng ký thành công"
    }
### POST localhost:8000/api/v1/auth/signin
- **Mô tả**: Đăng nhập 
- **Request**: 
    ```json
    {
        "password": "Thanh2004@",
        "email":"thanh1232004@gmail.com"
    }
- **Response**:
    ```json
    {
        "accessToken": "eyJhbGciOiJIUzUxMiJ9.eyJyb2xlcyI6IlVTRVIiLCJzdWIiOiJ0aGFuaDEyMzIwMDRAZ21haWwuY29tIiwiaWF0IjoxNzUwMzA2NzMwLCJleHAiOjE3NTAzMDc2MzB9.L3tosUROj3ewiDmU1veYOtTm-cNYNe77f6qJdLo90o8EpLHGd2dUuihS1aLQnISD6ZJ6Lazfy0C6UwpMUWrcQA",
        "refreshToken": "319122c6-e05e-429f-a058-b2d7dadd8613",
        "userId": 1,
        "email": "thanh1232004@gmail.com"
    }