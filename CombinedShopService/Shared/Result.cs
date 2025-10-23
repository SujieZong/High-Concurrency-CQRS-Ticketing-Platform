namespace CombinedShopService.Shared;


public class Result<T>
{

    public bool IsSuccess { get; }
    public String? Message { get; }
    public string? ErrorCode { get; }
    public IReadOnlyList<string>? Errors { get; } // make the Error String readonly
    public T? Data { get; }


    private Result(bool isSuccess, T? data, string? message, string? errorCode, IReadOnlyList<string>? errors)
    {
        IsSuccess = isSuccess;
        Data = data;
        Message = message;
        ErrorCode = errorCode;
        Errors = errors ?? Array.Empty<string>();
    }

    public static Result<T> Success(T data, string? message = "OK")
        => new(true, data, message, null, null);

    public static Result<T> Failure(
        string message,
        string? errorCode = null,
        IEnumerable<string>? errors = null)
        => new(false, default, message, errorCode, errors?.ToArray());
}