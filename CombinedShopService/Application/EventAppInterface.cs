using System.IO.Pipelines;

namespace CombinedShopService.Application;


public interface IEventService
{

    // get event by id
    Task<Result<EventDetailDto>> GetEventByIdAsync(int eventId, CancellationToken ct = default);

    // search Game by Category, by Tag ...
    Task<Result<EventDetailDto>> SearchGameAsync(
        int? categoryId = null,
        int[]? tagIds = null,
        string? city = null,
        DateOnly? from = null,
        DateOnly? to = null
    );

    // check if event exists
    Task<bool> EventExistCheck(int eventId, CancellationToken ct = default);

}