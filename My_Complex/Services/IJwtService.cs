using System;

namespace My_Complex.Services
{
    public interface IJwtService
    {
        string GenerateToken(string email, string role, out DateTime expiresAt);
    }
}
