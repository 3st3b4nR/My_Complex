using Microsoft.EntityFrameworkCore;
using My_Comunicacion.Config;
using My_Comunicacion.Services;

var builder = WebApplication.CreateBuilder(args);

builder.Services.AddDbContext<ComunicacionDbContext>(options =>
    options.UseNpgsql(builder.Configuration.GetConnectionString("DefaultConnection")));

builder.Services.AddScoped<ComunicacionService>();
builder.Services.AddMemoryCache();

builder.Services.AddControllers();
builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen();

var app = builder.Build();

app.UseSwagger();
app.UseSwaggerUI();
app.MapControllers();
app.Run();
